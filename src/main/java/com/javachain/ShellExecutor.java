package com.javachain;

import com.javachain.service.MiningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ShellExecutor {

    private final MiningService service;

    @Autowired
    public ShellExecutor(MiningService service) {
        this.service = service;
    }

    @ShellMethod("Work in progress")
    public String miner(
            @ShellOption String data,
            @ShellOption int difficulty) {
        // invoke service
        return service.mineNonce(data, difficulty);
    }
}
