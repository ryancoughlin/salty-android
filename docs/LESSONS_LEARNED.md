# Lessons Learned

## ANR / Startup Hang (March 2026)

**Symptom:** App loaded data successfully but map never panned to region. Tapping caused ANR.

1. **OpenGL calls must happen on the GL thread.** `uploadFrame()` called `glGenTextures`/`glTexImage2D` from a Kotlin IO coroutine — but the GL context only lives on Mapbox's render thread. On the emulator this hung silently; on a real device it would fail or crash. Fix: queue CPU data from any thread, drain the queue inside `render()` where the GL context exists.

2. **"Skipped N frames" is a symptom, not the disease.** We chased dispatcher fixes (`Dispatchers.IO`, dedup guards, StateFlow refactors) for two sessions. These were real problems worth fixing, but they masked the true blocker: a native JNI call that never returned. Always verify the last log line actually printed before assuming the next operation is the bottleneck.

3. **iOS Metal != Android OpenGL threading.** Metal uses command queues (submit from any thread, GPU executes async). OpenGL ES is thread-bound (context lives on one thread, all GL calls must happen there). When porting iOS GPU code to Android, never assume the same threading model works — always defer GL work to the render callback.
