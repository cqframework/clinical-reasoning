package org.opencds.cqf.cql.evaluator.cli.command;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

import org.opencds.cqf.cql.evaluator.cli.Main;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "argfile")
public class ArgFileCommand implements Callable<Integer> {

    @Parameters(arity = "1", description = "file containing arguments")
    File[] files;
    
    @Override
    public Integer call() throws Exception {
        List<String> args = Files.readAllLines(files[0].toPath());
        return Main.run(args.toArray(new String[args.size()]));
    }
    
}
