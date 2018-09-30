package com.ruscello.start;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

// Ruscello Commands
// help
// list streams (how to page through)
// stream meta data
// stream get by key
// stream get by range
// create stream
// delete stream (if we want to support)
// start ruscello
// stop ruscello
// restart ruscello
// scavenage

// commands from other projects kafka? heron? samza? cockroachdb? accumulo? redis?


// Entry point to starting server
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static ClassLoader classLoader;
    private static Class<?> vfsClassLoader;
//    private static Map<String, KeywordExecutable> servicesMap;

    public static void main(final String[] args) {

    }





    // From apache accumulo (https://github.com/apache/accumulo)
    // https://github.com/apache/accumulo/blob/b05611758b0ee0cb3ac14ed4eaeec116b92d8378/start/src/main/java/org/apache/accumulo/start/Main.java
//    public static synchronized ClassLoader getClassLoader() {
//        if (classLoader == null) {
//            try {
//                classLoader = (ClassLoader) getVFSClassLoader().getMethod("getClassLoader").invoke(null);
//                Thread.currentThread().setContextClassLoader(classLoader);
//            } catch (ClassNotFoundException | IOException | IllegalAccessException
//                    | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
//                    | SecurityException e) {
//                log.error("Problem initializing the class loader", e);
//                System.exit(1);
//            }
//        }
//        return classLoader;
//    }
//
//    private static void execKeyword(final KeywordExecutable keywordExec, final String[] args) {
//        Runnable r = () -> {
//            try {
//                keywordExec.execute(args);
//            } catch (Exception e) {
//                die(e);
//            }
//        };
//        startThread(r, keywordExec.keyword());
//    }
//
//    private static void execMainClassName(final String className, final String[] args) {
//        Class<?> classWithMain = null;
//        try {
//            classWithMain = getClassLoader().loadClass(className);
//        } catch (ClassNotFoundException cnfe) {
//            System.out.println("Invalid argument: Java <main class> '" + className
//                    + "' was not found.  Please use the wholly qualified package name.");
//            printUsage();
//            System.exit(1);
//        }
//        execMainClass(classWithMain, args);
//    }
//
//    public static void execMainClass(final Class<?> classWithMain, final String[] args) {
//        Method main = null;
//        try {
//            main = classWithMain.getMethod("main", args.getClass());
//        } catch (Throwable t) {
//            log.error("Could not run main method on '" + classWithMain.getName() + "'.", t);
//        }
//        if (main == null || !Modifier.isPublic(main.getModifiers())
//                || !Modifier.isStatic(main.getModifiers())) {
//            System.out.println(classWithMain.getName()
//                    + " must implement a public static void main(String args[]) method");
//            System.exit(1);
//        }
//        final Method finalMain = main;
//        Runnable r = () -> {
//            try {
//                finalMain.invoke(null, (Object) args);
//            } catch (InvocationTargetException e) {
//                if (e.getCause() != null) {
//                    die(e.getCause());
//                } else {
//                    // Should never happen, but check anyway.
//                    die(e);
//                }
//            } catch (Exception e) {
//                die(e);
//            }
//        };
//        startThread(r, classWithMain.getName());
//    }
//
//    public static String[] stripArgs(final String[] originalArgs, int numToStrip) {
//        int newSize = originalArgs.length - numToStrip;
//        String newArgs[] = new String[newSize];
//        System.arraycopy(originalArgs, numToStrip, newArgs, 0, newSize);
//        return newArgs;
//    }
//
//    private static void startThread(final Runnable r, final String name) {
//        Thread t = new Thread(r, name);
//        t.setContextClassLoader(getClassLoader());
//        t.start();
//    }
//
//    /**
//     * Print a stack trace to stderr and exit with a non-zero status.
//     *
//     * @param t
//     *          The {@link Throwable} containing a stack trace to print.
//     */
//    private static void die(final Throwable t) {
//        log.error("Thread '" + Thread.currentThread().getName() + "' died.", t);
//        System.exit(1);
//    }
//
//    public static void printCommands(TreeSet<KeywordExecutable> set, UsageGroup group) {
//        set.stream().filter(e -> e.usageGroup() == group)
//                .forEach(ke -> System.out.printf("  %-30s %s\n", ke.usage(), ke.description()));
//    }
//
//    public static void printUsage() {
//        TreeSet<KeywordExecutable> executables = new TreeSet<>(
//                Comparator.comparing(KeywordExecutable::keyword));
//        executables.addAll(getExecutables(getClassLoader()).values());
//
//        System.out.println("\nUsage: accumulo <command> [--help] (<argument> ...)\n\n"
//                + "  --help   Prints usage for specified command");
//        System.out.println("\nCore Commands:");
//        printCommands(executables, UsageGroup.CORE);
//
//        System.out.println(
//                "  <main class> args              Runs Java <main class> located on Accumulo classpath");
//
//        System.out.println("\nProcess Commands:");
//        printCommands(executables, UsageGroup.PROCESS);
//
//        System.out.println("\nOther Commands:");
//        printCommands(executables, UsageGroup.OTHER);
//
//        System.out.println();
//    }
//
//    public static synchronized Map<String,KeywordExecutable> getExecutables(final ClassLoader cl) {
//        if (servicesMap == null) {
//            servicesMap = checkDuplicates(ServiceLoader.load(KeywordExecutable.class, cl));
//        }
//        return servicesMap;
//    }
//
//    public static Map<String,KeywordExecutable> checkDuplicates(
//            final Iterable<? extends KeywordExecutable> services) {
//        TreeSet<String> blacklist = new TreeSet<>();
//        TreeMap<String,KeywordExecutable> results = new TreeMap<>();
//        for (KeywordExecutable service : services) {
//            String keyword = service.keyword();
//            if (blacklist.contains(keyword)) {
//                // subsequent times a duplicate is found, just warn and exclude it
//                warnDuplicate(service);
//            } else if (results.containsKey(keyword)) {
//                // the first time a duplicate is found, blacklist it and warn
//                blacklist.add(keyword);
//                warnDuplicate(results.remove(keyword));
//                warnDuplicate(service);
//            } else {
//                // first observance of this keyword, so just add it to the list
//                results.put(service.keyword(), service);
//            }
//        }
//        return Collections.unmodifiableSortedMap(results);
//    }
//
//    private static void warnDuplicate(final KeywordExecutable service) {
//        log.warn("Ambiguous duplicate binding for keyword '{}' found: {}", service.keyword(),
//                service.getClass().getName());
//    }



}
