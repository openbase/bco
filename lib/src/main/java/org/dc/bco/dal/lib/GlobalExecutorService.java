/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public final class GlobalExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExecutorService.class);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private GlobalExecutorService() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                logger.info("Executor service is shutting down...");
                EXECUTOR_SERVICE.shutdown();
            }
        });
    }

    public static <V> Future<V> submit(Callable<V> task) {
        return EXECUTOR_SERVICE.submit(task);
    }

//    static class LoggingThreadFactory implements ThreadFactory {
//
//        private static final AtomicInteger poolNumber = new AtomicInteger(1);
//        private final ThreadGroup group;
//        private final AtomicInteger threadNumber = new AtomicInteger(1);
//        private final String namePrefix;
//
//        LoggingThreadFactory() {
//            SecurityManager s = System.getSecurityManager();
//            group = (s != null) ? s.getThreadGroup()
//                    : Thread.currentThread().getThreadGroup();
//            namePrefix = "pool-"
//                    + poolNumber.getAndIncrement()
//                    + "-thread-";
//        }
//
//        @Override
//        public Thread newThread(Runnable r) {
//            Thread t = new Thread(group, r,
//                    namePrefix + threadNumber.getAndIncrement(),
//                    0) {
//
//                        @Override
//                        @SuppressWarnings("CallToThreadRun")
//                        public void run() {
//                            try {
//                                super.run();
//                            } catch (Exception ex) {
//                                throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
//                            }
//                        }
//
//                    };
//            if (t.isDaemon()) {
//                t.setDaemon(false);
//            }
//            if (t.getPriority() != Thread.NORM_PRIORITY) {
//                t.setPriority(Thread.NORM_PRIORITY);
//            }
//            return t;
//        }
//    }
}
