package com.kingjoshdavid.funfection.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;
import com.kingjoshdavid.funfection.data.local.VirusEntity;
import com.kingjoshdavid.funfection.engine.VirusFactory;
import com.kingjoshdavid.funfection.model.Chaos;
import com.kingjoshdavid.funfection.model.Infectivity;
import com.kingjoshdavid.funfection.model.Resilience;
import com.kingjoshdavid.funfection.model.Virus;
import com.kingjoshdavid.funfection.model.VirusOrigin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class VirusRepository {

    public interface ResultCallback<T> {
        void onResult(T result);
    }

    public interface CompletionCallback {
        void onComplete();
    }

    // Kept for JVM unit tests where Android Room is unavailable.
    private static final List<Virus> COLLECTION = new ArrayList<>();

    private static final String IO_THREAD_NAME = "virus-repository-io";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, IO_THREAD_NAME));

    public enum PurgeResult {
        REMOVED,
        MISSING,
        BLOCKED_LAST
    }

    private VirusRepository() {
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
        DatabaseProvider.get(context);
        ensureSeeded();
    }

    public static void ensureSeeded() {
        if (isUsingInMemoryFallback()) {
            if (COLLECTION.isEmpty()) {
                COLLECTION.addAll(VirusFactory.createStarterViruses());
            }
            return;
        }

        runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            if (database.virusDao().count() == 0) {
                List<Virus> starters = VirusFactory.createStarterViruses();
                long now = System.currentTimeMillis();
                int size = starters.size();
                for (int index = 0; index < size; index++) {
                    Virus virus = starters.get(index);
                    VirusEntity entity = toEntity(virus);
                    entity.createdAt = now + (size - index);
                    database.virusDao().upsert(entity);
                }
            }
            return null;
        });
    }

    public static List<Virus> getViruses() {
        ensureSeeded();
        if (isUsingInMemoryFallback()) {
            return new ArrayList<>(COLLECTION);
        }
        return runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return new ArrayList<>();
            }
            List<VirusEntity> entities = database.virusDao().getAll();
            List<Virus> viruses = new ArrayList<>(entities.size());
            for (VirusEntity entity : entities) {
                viruses.add(toDomain(entity));
            }
            return viruses;
        });
    }

    public static void getVirusesAsync(ResultCallback<List<Virus>> callback) {
        runOnIoAsync(VirusRepository::getViruses, callback);
    }

    public static Virus getVirusById(String id) {
        ensureSeeded();
        if (isUsingInMemoryFallback()) {
            for (Virus virus : COLLECTION) {
                if (virus.getId().equals(id)) {
                    return virus;
                }
            }
            return null;
        }
        return runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            VirusEntity entity = database.virusDao().findById(id);
            return entity == null ? null : toDomain(entity);
        });
    }

    public static void getVirusByIdAsync(String id, ResultCallback<Virus> callback) {
        runOnIoAsync(() -> getVirusById(id), callback);
    }

    public static void addVirus(Virus virus) {
        ensureSeeded();
        if (isUsingInMemoryFallback()) {
            COLLECTION.add(0, virus);
            FriendsRepository.recordDiscovery(virus);
            return;
        }
        runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            VirusEntity entity = toEntity(virus);
            entity.createdAt = System.currentTimeMillis();
            database.virusDao().upsert(entity);
            FriendsRepository.recordDiscovery(virus);
            return null;
        });
    }

    public static void addVirusAsync(Virus virus, CompletionCallback callback) {
        runOnIoAsync(() -> addVirus(virus), callback);
    }

    public static void replaceVirus(Virus virus) {
        ensureSeeded();
        if (isUsingInMemoryFallback()) {
            for (int index = 0; index < COLLECTION.size(); index++) {
                if (COLLECTION.get(index).getId().equals(virus.getId())) {
                    COLLECTION.set(index, virus);
                    FriendsRepository.recordDiscovery(virus);
                    return;
                }
            }
            COLLECTION.add(0, virus);
            FriendsRepository.recordDiscovery(virus);
            return;
        }
        runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            VirusEntity existing = database.virusDao().findById(virus.getId());
            VirusEntity entity = toEntity(virus);
            entity.createdAt = existing == null ? System.currentTimeMillis() : existing.createdAt;
            database.virusDao().upsert(entity);
            FriendsRepository.recordDiscovery(virus);
            return null;
        });
    }

    public static boolean removeVirusById(String id) {
        return purgeVirusById(id) == PurgeResult.REMOVED;
    }

    public static PurgeResult getPurgeStatus(String id) {
        ensureSeeded();
        if (id == null || id.trim().isEmpty()) {
            return PurgeResult.MISSING;
        }

        if (isUsingInMemoryFallback()) {
            return findVirusIndexById(id) >= 0 ? PurgeResult.REMOVED : PurgeResult.MISSING;
        }

        return runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return PurgeResult.MISSING;
            }
            boolean exists = database.virusDao().findById(id) != null;
            if (!exists) {
                return PurgeResult.MISSING;
            }
            return PurgeResult.REMOVED;
        });
    }

    public static void getPurgeStatusAsync(String id, ResultCallback<PurgeResult> callback) {
        runOnIoAsync(() -> getPurgeStatus(id), callback);
    }

    public static PurgeResult purgeVirusById(String id) {
        ensureSeeded();
        if (id == null || id.trim().isEmpty()) {
            return PurgeResult.MISSING;
        }

        if (isUsingInMemoryFallback()) {
            int index = findVirusIndexById(id);
            if (index < 0) {
                return PurgeResult.MISSING;
            }
            COLLECTION.remove(index);
            return PurgeResult.REMOVED;
        }

        return runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return PurgeResult.MISSING;
            }
            return database.virusDao().deleteById(id) > 0
                ? PurgeResult.REMOVED
                : PurgeResult.MISSING;
        });
    }

    public static void purgeVirusByIdAsync(String id, ResultCallback<PurgeResult> callback) {
        runOnIoAsync(() -> purgeVirusById(id), callback);
    }

    private static int findVirusIndexById(String id) {
        if (id == null) {
            return -1;
        }
        for (int index = 0; index < COLLECTION.size(); index++) {
            if (COLLECTION.get(index).getId().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    public static List<Virus> pickByIds(List<String> ids) {
        ensureSeeded();
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Virus> matches = new ArrayList<>();
        for (String id : ids) {
            Virus virus = getVirusById(id);
            if (virus != null) {
                matches.add(virus);
            }
        }
        return matches;
    }

    private static boolean isUsingInMemoryFallback() {
        return DatabaseProvider.getIfInitialized() == null;
    }

    private static <T> T runOnIo(Callable<T> callable) {
        if (isIoThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new IllegalStateException("VirusRepository operation failed", e);
            }
        }
        Future<T> future = IO.submit(callable);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("VirusRepository operation interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("VirusRepository operation failed", e);
        }
    }

    private static <T> void runOnIoAsync(Callable<T> callable, ResultCallback<T> callback) {
        IO.execute(() -> {
            T result;
            try {
                result = callable.call();
            } catch (Exception e) {
                throw new IllegalStateException("VirusRepository operation failed", e);
            }
            if (callback != null) {
                postToMain(() -> callback.onResult(result));
            }
        });
    }

    private static void runOnIoAsync(Runnable runnable, CompletionCallback callback) {
        IO.execute(() -> {
            runnable.run();
            if (callback != null) {
                postToMain(callback::onComplete);
            }
        });
    }

    private static boolean isIoThread() {
        return IO_THREAD_NAME.equals(Thread.currentThread().getName());
    }

    private static void postToMain(Runnable runnable) {
        try {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper == null || Thread.currentThread() == mainLooper.getThread()) {
                runnable.run();
                return;
            }
            new Handler(mainLooper).post(runnable);
        } catch (RuntimeException ignored) {
            // Local JVM tests run without Android loopers; execute inline there.
            runnable.run();
        }
    }

    private static VirusEntity toEntity(Virus virus) {
        VirusEntity entity = new VirusEntity();
        entity.id = virus.getId();
        entity.prefix = virus.getPrefix();
        entity.suffix = virus.getSuffix();
        entity.family = virus.getFamily();
        entity.carrier = virus.getCarrier();
        entity.infectivity = virus.getInfectivity().score();
        entity.resilience = virus.getResilience().score();
        entity.chaos = virus.getChaos().score();
        entity.mutation = virus.hasMutation();
        entity.genome = virus.getGenome();
        entity.originSummary = virus.getOrigin();
        entity.originPayload = virus.getOriginInfo().toSharePayload();
        entity.generation = virus.getGeneration();
        entity.productionContext = virus.getProductionContext();
        return entity;
    }

    private static Virus toDomain(VirusEntity entity) {
        VirusOrigin origin = VirusOrigin.fromSharePayload(entity.originPayload);
        if (origin == null) {
            origin = VirusOrigin.legacy(entity.originSummary);
        }
        return new Virus(entity.id,
                entity.prefix,
                entity.suffix,
                entity.family,
                entity.carrier,
                Infectivity.rate(entity.infectivity),
                Resilience.of(entity.resilience),
                Chaos.level(entity.chaos),
                entity.mutation,
                entity.genome,
                origin,
                entity.generation,
                entity.productionContext);
    }
}
