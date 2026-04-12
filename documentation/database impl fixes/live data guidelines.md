# LiveData Guidelines (Java + Room)

## Core Principles

- Never block the UI thread (no `Future.get()`, no synchronous DB calls)
- Prefer observable data streams over one-time fetches
- Keep threading out of the UI layer
- Let Room + LiveData handle async behavior for reads

---

## DAO Layer

### Return LiveData for queries

```java
@Dao
public interface VirusDao {

    @Query("SELECT * FROM virus ORDER BY created_at DESC")
    LiveData<List<Virus>> getAllViruses();

    @Query("SELECT * FROM virus WHERE id = :id LIMIT 1")
    LiveData<Virus> getVirusById(String id);
}
```

### Rules

* Use `LiveData<T>` for all UI-driven reads
* Do NOT wrap LiveData in executors
* Do NOT return raw lists for UI consumption
* Keep queries simple and deterministic

---

## Repository Layer

### Pass through LiveData directly

```java
public class VirusRepository {

    private final VirusDao virusDao;

    public VirusRepository(VirusDao virusDao) {
        this.virusDao = virusDao;
    }

    public LiveData<List<Virus>> getAllViruses() {
        return virusDao.getAllViruses();
    }
}
```

### Rules

* Repositories expose LiveData unchanged
* Do NOT call `.getValue()` in repository
* Do NOT block waiting for results
* Avoid converting LiveData into synchronous values
* Repository should remain a thin abstraction over DAO

---

## Writes (Insert / Update / Delete)

### Always run writes off the main thread

```java
public class VirusRepository {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final VirusDao virusDao;

    public void insertVirus(Virus virus) {
        executor.execute(() -> virusDao.insert(virus));
    }

    public void deleteVirus(Virus virus) {
        executor.execute(() -> virusDao.delete(virus));
    }
}
```

### Rules

* Always use background thread for writes
* Room will throw if DB access happens on main thread
* Do NOT return LiveData from write operations

---

## ViewModel Layer

### Hold and expose LiveData

```java
public class VirusViewModel extends ViewModel {

    private final LiveData<List<Virus>> viruses;

    public VirusViewModel(VirusRepository repository) {
        this.viruses = repository.getAllViruses();
    }

    public LiveData<List<Virus>> getViruses() {
        return viruses;
    }
}
```

### Rules

* ViewModel is the source of truth for UI data
* Expose immutable LiveData only
* Do NOT expose DAO directly to UI
* Do NOT perform heavy logic in UI layer

---

## UI Layer (Activity / Fragment)

### Observe LiveData

```java
viewModel.getViruses().observe(getViewLifecycleOwner(), viruses -> {
    adapter.submitList(viruses);
});
```

### Rules

* Always observe with a lifecycle owner
* UI reacts to data changes
* Do NOT manually refresh data
* Do NOT fetch synchronously

---

## Transformations (Optional)

### Map LiveData

```java
LiveData<Integer> virusCount =
    Transformations.map(
        repository.getAllViruses(),
        list -> list.size()
    );
```

### Rules

* Use for lightweight UI transformations
* Avoid heavy computation inside transformations
* Keep transformations in ViewModel when possible

---

## Loading & State Handling

LiveData does NOT inherently represent loading or error state.

### Example wrapper

```java
public class UiState<T> {

    public final boolean isLoading;
    public final T data;
    public final String error;

    public UiState(boolean isLoading, T data, String error) {
        this.isLoading = isLoading;
        this.data = data;
        this.error = error;
    }
}
```

### Rules

* Model loading explicitly when needed
* Do NOT assume LiveData implies loading state
* Combine LiveData with state objects for complex screens

---

## One-Time Events (Navigation, Toasts)

LiveData re-emits on configuration changes → can cause duplicate events.

### Event wrapper

```java
public class Event<T> {

    private boolean handled = false;
    private final T content;

    public Event(T content) {
        this.content = content;
    }

    public T getIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
```

### Rules

* Use Event wrapper for one-time actions
* Do NOT use plain LiveData for navigation or toasts

---

## Testing

### Use in-memory Room database

* Use `Room.inMemoryDatabaseBuilder(...)`
* Observe LiveData with test utilities (or blocking observers)

### Rules

* Test DAO + LiveData integration
* Verify ordering, mapping, and constraints
* Do NOT rely only on non-Room tests

---

## Anti-Patterns to Avoid

* Blocking calls (`future.get()`, `join()`)
* Converting LiveData into synchronous results
* Manual threading for reads
* Observing LiveData without lifecycle awareness
* Business logic inside Activities or Fragments

---

## Recommended Architecture

UI (Activity / Fragment)
↓ observe
ViewModel
↓ exposes LiveData
Repository
↓ delegates
DAO (Room)
↓
Database

---

## Summary

* Reads → LiveData from Room (automatic async)
* Writes → background executor
* UI → observe only, never block
* ViewModel → owns UI data
* Repository → clean abstraction, no blocking
