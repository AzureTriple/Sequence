package sequence;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;

/**A class which simply holds a constant {@linkplain Cleaner}.*/
final class CleaningUtil {
    private CleaningUtil() {}
    
    private static final Cleaner CLEANER = Cleaner.create();
    static {
        final Runtime rt = Runtime.getRuntime();
        rt.addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Spam the gc so that the cleaners run.
                rt.gc(); rt.gc();
                rt.gc(); rt.gc();
                rt.gc(); rt.gc();
            }
        });
    }
    static Cleanable register(final Object obj,final Runnable action) {return CLEANER.register(obj,action);}
}