package com.javachain.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Test helper for spawning real JVM processes (the standalone verifier, peer nodes)
 * and collecting their stdout plus exit code.
 */
final class ProcessLauncher {

    private ProcessLauncher() {
    }

    static ProcessResult launch(Class<?> mainClass, long timeoutSeconds, String... args)
            throws Exception {
        String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        // surefire may hand us a manifest-only booter jar as the class path; appending
        // target/classes guarantees our own tools resolve in every setup.
        String classPath = System.getProperty("java.class.path")
                + File.pathSeparator + "target" + File.separator + "classes";

        String[] command = new String[args.length + 4];
        command[0] = javaBin;
        command[1] = "-cp";
        command[2] = classPath;
        command[3] = mainClass.getName();
        System.arraycopy(args, 0, command, 4, args.length);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("process " + mainClass.getSimpleName()
                    + " did not finish within " + timeoutSeconds + "s:\n" + output);
        }
        return new ProcessResult(process.exitValue(), output.toString());
    }

    static final class ProcessResult {
        final int exitCode;
        final String output;

        ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
