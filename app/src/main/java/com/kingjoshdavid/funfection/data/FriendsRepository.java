package com.kingjoshdavid.funfection.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.kingjoshdavid.funfection.data.local.DatabaseProvider;
import com.kingjoshdavid.funfection.data.local.FriendEntity;
import com.kingjoshdavid.funfection.data.local.FunfectionDatabase;
import com.kingjoshdavid.funfection.model.Friend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, IO_THREAD_NAME));

    private FriendsRepository() {
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
        DatabaseProvider.get(context);
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
        if (DatabaseProvider.getIfInitialized() == null) {
            deleteFriend(friend.getId());
            FRIENDS.add(0, friend);
            return;
        }
        runOnIo(() -> {
            FunfectionDatabase database = DatabaseProvider.getIfInitialized();
            if (database == null) {
                return null;
            }
            FriendEntity existing = database.friendDao().findById(friend.getId());
            FriendEntity entity = toEntity(friend);
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
        if (DatabaseProvider.getIfInitialized() == null) {
            for (int index = 0; index < FRIENDS.size(); index++) {
                if (FRIENDS.get(index).getId().equals(id)) {
                    FRIENDS.remove(index);
                    return true;
                }
            }
            return false;
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
        return new Friend(entity.id, entity.displayName, entity.inviteCode);
    }

    private static FriendEntity toEntity(Friend friend) {
        FriendEntity entity = new FriendEntity();
        entity.id = friend.getId();
        entity.displayName = friend.getDisplayName();
        entity.inviteCode = friend.getInviteCode();
        return entity;
    }
}
