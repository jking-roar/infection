package com.kingjoshdavid.funfection.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FriendEntity;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;
import com.kingjoshdavid.funfection.model.Friend;
import com.kingjoshdavid.funfection.model.Virus;
import com.kingjoshdavid.funfection.model.VirusOrigin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class FriendsRepository {

    public interface ResultCallback<T> {
        void onResult(T result);
    }

    public interface CompletionCallback {
        void onComplete();
    }

    private static final List<Friend> FRIENDS = new ArrayList<>();
    private static final String IO_THREAD_NAME = "friends-repository-io";
    private static final String SIMULATED_FLAG = "[SIMULATED]";
    private static final List<Friend> KNOWN_SCIENTISTS = createKnownScientists();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, IO_THREAD_NAME));

    private FriendsRepository() {
    }

    public static void initialize(Context context) {
        if (context != null) {
            DatabaseProvider.get(context);
        }
        seedKnownScientists();
    }

    public static List<Friend> getFriends() {
        if (DatabaseProvider.getIfInitialized() == null) {
            return new ArrayList<>(FRIENDS);
        }
        return runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return new ArrayList<>();
            }
            List<FriendEntity> entities = database.friendDao().getAll();
            List<Friend> friends = new ArrayList<>(entities.size());
            for (FriendEntity entity : entities) {
                friends.add(toDomain(entity));
            }
            return friends;
        });
    }

    public static void getFriendsAsync(ResultCallback<List<Friend>> callback) {
        runOnIoAsync(FriendsRepository::getFriends, callback);
    }

    public static Friend getFriendById(String id) {
        if (DatabaseProvider.getIfInitialized() == null) {
            for (Friend friend : FRIENDS) {
                if (friend.getId().equals(id)) {
                    return friend;
                }
            }
            return null;
        }

        return runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            FriendEntity entity = database.friendDao().findById(id);
            return entity == null ? null : toDomain(entity);
        });
    }

    public static void getFriendByIdAsync(String id, ResultCallback<Friend> callback) {
        runOnIoAsync(() -> getFriendById(id), callback);
    }

    public static void saveFriend(Friend friend) {
        if (friend == null) {
            return;
        }
        Friend normalized = normalizeFriend(friend);
        if (DatabaseProvider.getIfInitialized() == null) {
            deleteFriendInternal(normalized.getId());
            FRIENDS.add(0, normalized);
            return;
        }
        runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            FriendEntity existing = database.friendDao().findById(normalized.getId());
            FriendEntity entity = toEntity(normalized);
            entity.createdAt = existing == null ? System.currentTimeMillis() : existing.createdAt;
            database.friendDao().upsert(entity);
            return null;
        });
    }

    public static void saveFriendAsync(Friend friend, CompletionCallback callback) {
        runOnIoAsync(() -> saveFriend(friend), callback);
    }

    public static boolean deleteFriend(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        Friend existing = getFriendById(id);
        if (existing != null && existing.isProtectedProfile()) {
            return false;
        }
        if (DatabaseProvider.getIfInitialized() == null) {
            return deleteFriendInternal(id);
        }
        return runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return false;
            }
            return database.friendDao().deleteById(id) > 0;
        });
    }

    public static void deleteFriendAsync(String id, ResultCallback<Boolean> callback) {
        runOnIoAsync(() -> deleteFriend(id), callback);
    }

    public static List<Friend> pickByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Friend> friends = new ArrayList<>();
        for (String id : ids) {
            Friend friend = getFriendById(id);
            if (friend != null) {
                friends.add(friend);
            }
        }
        return friends;
    }

    public static void recordDiscovery(Virus virus) {
        if (virus == null) {
            return;
        }
        recordDiscovery(virus.getOriginInfo());
    }

    public static void recordDiscovery(VirusOrigin origin) {
        if (origin == null) {
            return;
        }
        runOnIo(() -> {
            LinkedHashMap<String, DiscoveredFriend> discovered = collectDiscoveredFriends(origin);
            for (DiscoveredFriend candidate : discovered.values()) {
                upsertDiscoveredFriend(candidate);
            }
            return null;
        });
    }

    private static <T> T runOnIo(Callable<T> callable) {
        if (isIoThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new IllegalStateException("FriendsRepository operation failed", e);
            }
        }
        Future<T> future = IO.submit(callable);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FriendsRepository operation interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("FriendsRepository operation failed", e);
        }
    }

    private static <T> void runOnIoAsync(Callable<T> callable, ResultCallback<T> callback) {
        IO.execute(() -> {
            T result;
            try {
                result = callable.call();
            } catch (Exception e) {
                throw new IllegalStateException("FriendsRepository operation failed", e);
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
            runnable.run();
        }
    }

    private static Friend toDomain(FriendEntity entity) {
        return new Friend(entity.id,
                entity.displayName,
                entity.inviteCode,
                entity.displayNameOverride,
                entity.notes,
                entity.description,
                entity.protectedProfile,
                decodeHandleHistory(entity.handleHistoryPayload));
    }

    private static FriendEntity toEntity(Friend friend) {
        FriendEntity entity = new FriendEntity();
        entity.id = friend.getId();
        entity.displayName = friend.getDisplayName();
        entity.inviteCode = friend.getInviteCode();
        entity.displayNameOverride = friend.getDisplayNameOverride();
        entity.notes = friend.isProtectedProfile() ? "" : friend.getNotes();
        entity.description = friend.getDescription();
        entity.protectedProfile = friend.isProtectedProfile();
        entity.handleHistoryPayload = encodeHandleHistory(friend.getHandleHistory());
        return entity;
    }

    private static Friend normalizeFriend(Friend friend) {
        return new Friend(friend.getId(),
                friend.getDisplayName(),
                friend.getInviteCode(),
                friend.getDisplayNameOverride(),
                friend.getNotes(),
                friend.getDescription(),
                friend.isProtectedProfile(),
                friend.getHandleHistory());
    }

    private static LinkedHashMap<String, DiscoveredFriend> collectDiscoveredFriends(VirusOrigin origin) {
        LinkedHashMap<String, DiscoveredFriend> discovered = new LinkedHashMap<>();
        for (VirusOrigin.PatientZero patientZero : origin.getPatientZeros()) {
            DiscoveredFriend candidate = toDiscoveredFriend(patientZero);
            if (candidate != null) {
                discovered.put(candidate.id, candidate);
            }
        }
        DiscoveredFriend source = toDiscoveredFriend(origin.getDirectSource());
        if (source != null) {
            discovered.put(source.id, source);
        }
        return discovered;
    }

    private static DiscoveredFriend toDiscoveredFriend(VirusOrigin.Source source) {
        if (source == null || source.getId().trim().isEmpty() || source.getDisplayName().trim().isEmpty()) {
            return null;
        }
        if (isCurrentUser(source.getId()) || isKnownScientistId(source.getId()) || !source.isRealFriend()) {
            return null;
        }
        return buildDiscoveredFriend(source.getId(), source.getDisplayName());
    }

    private static DiscoveredFriend toDiscoveredFriend(VirusOrigin.PatientZero patientZero) {
        if (patientZero == null || patientZero.getId().trim().isEmpty() || patientZero.getDisplayName().trim().isEmpty()) {
            return null;
        }
        if (isCurrentUser(patientZero.getId()) || isKnownScientistId(patientZero.getId())) {
            return null;
        }
        return buildDiscoveredFriend(patientZero.getId(), patientZero.getDisplayName());
    }

    private static DiscoveredFriend buildDiscoveredFriend(String id, String displayName) {
        return new DiscoveredFriend(id, displayName, false, "");
    }

    private static void upsertDiscoveredFriend(DiscoveredFriend candidate) {
        Friend existing = getFriendById(candidate.id);
        if (existing == null) {
            saveFriend(new Friend(candidate.id,
                    candidate.displayName,
                    "",
                    "",
                    "",
                    candidate.description,
                    candidate.protectedProfile,
                    Collections.emptyList()));
            return;
        }

        List<String> history = new ArrayList<>(existing.getHandleHistory());
        String currentDisplayName = existing.getDisplayName();
        if (shouldArchiveHandle(currentDisplayName, candidate.displayName)
                && !containsHandle(history, currentDisplayName)) {
            history.add(currentDisplayName);
        }

        saveFriend(new Friend(existing.getId(),
                candidate.displayName,
                existing.getInviteCode(),
                existing.getDisplayNameOverride(),
                existing.isProtectedProfile() || candidate.protectedProfile ? "" : existing.getNotes(),
                candidate.description.isEmpty() ? existing.getDescription() : candidate.description,
                existing.isProtectedProfile() || candidate.protectedProfile,
                history));
    }

    private static boolean shouldArchiveHandle(String previousDisplayName, String nextDisplayName) {
        String previous = normalizeHandle(previousDisplayName);
        String next = normalizeHandle(nextDisplayName);
        return !previous.isEmpty()
                && !next.isEmpty()
                && !previous.equalsIgnoreCase(next);
    }

    private static boolean containsHandle(List<String> history, String handle) {
        String normalizedHandle = normalizeHandle(handle);
        if (normalizedHandle.isEmpty()) {
            return false;
        }
        for (String existing : history) {
            if (normalizedHandle.equalsIgnoreCase(normalizeHandle(existing))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCurrentUser(String id) {
        try {
            String currentUserId = UserProfileRepository.getCurrentUser().getId();
            return currentUserId != null && currentUserId.equals(id);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isKnownScientistId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        for (Friend scientist : KNOWN_SCIENTISTS) {
            if (scientist.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeHandle(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static boolean deleteFriendInternal(String id) {
        for (int index = 0; index < FRIENDS.size(); index++) {
            if (FRIENDS.get(index).getId().equals(id)) {
                FRIENDS.remove(index);
                return true;
            }
        }
        return false;
    }

    private static String encodeHandleHistory(List<String> handleHistory) {
        if (handleHistory == null || handleHistory.isEmpty()) {
            return "";
        }
        StringBuilder payload = new StringBuilder();
        for (String handle : handleHistory) {
            String normalized = normalizeHandle(handle);
            if (normalized.isEmpty()) {
                continue;
            }
            if (payload.length() > 0) {
                payload.append('\n');
            }
            payload.append(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(normalized.getBytes(StandardCharsets.UTF_8)));
        }
        return payload.toString();
    }

    private static List<String> decodeHandleHistory(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> handles = new ArrayList<>();
        String[] lines = payload.split("\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(line), StandardCharsets.UTF_8);
                if (!containsHandle(handles, decoded)) {
                    handles.add(decoded);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed history entries from older or corrupted local rows.
            }
        }
        return handles;
    }

    private static void seedKnownScientists() {
        if (DatabaseProvider.getIfInitialized() == null) {
            for (Friend scientist : KNOWN_SCIENTISTS) {
                upsertKnownScientistInMemory(scientist);
            }
            return;
        }

        runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            for (Friend scientist : KNOWN_SCIENTISTS) {
                FriendEntity existing = database.friendDao().findById(scientist.getId());
                FriendEntity entity = toEntity(scientist);
                entity.createdAt = existing == null ? System.currentTimeMillis() : existing.createdAt;
                database.friendDao().upsert(entity);
            }
            return null;
        });
    }

    private static void upsertKnownScientistInMemory(Friend scientist) {
        for (int index = 0; index < FRIENDS.size(); index++) {
            if (FRIENDS.get(index).getId().equals(scientist.getId())) {
                FRIENDS.set(index, scientist);
                return;
            }
        }
        FRIENDS.add(scientist);
    }

    private static List<Friend> createKnownScientists() {
        List<Friend> scientists = new ArrayList<>();
        scientists.add(createKnownScientist("Professor Tesla",
                "A reclusive pioneer of unstable energy-based strains. Tesla's creations hum with latent power, often behaving unpredictably when exposed to other signals."));
        scientists.add(createKnownScientist("Doctor Curie",
                "A meticulous researcher of radiant mutations. Curie specializes in slow-evolving strains that intensify over time, rewarding patience—and punishing carelessness."));
        scientists.add(createKnownScientist("Doc Brown",
                "An erratic temporal theorist whose viruses don’t always follow linear progression. Effects may trigger early, late, or seemingly out of order."));
        scientists.add(createKnownScientist("Professor Xavier",
                "A strategist of cognitive contagions. Xavier engineers subtle strains that influence behavior, spreading quietly before revealing their full impact."));
        scientists.add(createKnownScientist("The Gutter Man",
                "A shadowy figure lurking beneath the system. His strains are crude, resilient, and disturbingly adaptive—thriving in neglected or chaotic environments."));
        return Collections.unmodifiableList(scientists);
    }

    private static Friend createKnownScientist(String displayName, String description) {
        return new Friend(scientistId(displayName),
                displayName,
                "",
                "",
                "",
                description,
                true,
                Collections.emptyList());
    }

    private static String scientistId(String displayName) {
        String sourceMoniker = normalizeHandle(displayName) + " " + SIMULATED_FLAG;
        return UUID.nameUUIDFromBytes(("fake:" + sourceMoniker).getBytes(StandardCharsets.UTF_8)).toString();
    }


    private static final class DiscoveredFriend {

        private final String id;
        private final String displayName;
        private final boolean protectedProfile;
        private final String description;

        private DiscoveredFriend(String id, String displayName, boolean protectedProfile, String description) {
            this.id = id == null ? UUID.randomUUID().toString() : id;
            this.displayName = normalizeHandle(displayName);
            this.protectedProfile = protectedProfile;
            this.description = description == null ? "" : description;
        }
    }
}
