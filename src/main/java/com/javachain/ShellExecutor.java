package com.javachain;

import com.javachain.dto.Block;
import com.javachain.persistence.FileChainStore;
import com.javachain.service.BlockService;
import com.javachain.service.MiningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;

@ShellComponent
public class ShellExecutor {

    private final MiningService miningService;
    private final BlockService blockService;

    @Autowired
    public ShellExecutor(MiningService service, BlockService blockService) {
        this.miningService = service;
        this.blockService = blockService;
    }

    @ShellMethod("Work in progress")
    public String miner(
            @ShellOption String data,
            @ShellOption int difficulty) {
        // invoke service
        return miningService.mineNonce(data, difficulty);
    }

    @ShellMethod("Verify a chain previously saved with FileChainStore")
    public String verifyFile(@ShellOption String file) {
        try {
            Block chainTip = FileChainStore.load(new File(file), Block.class);
            boolean valid = blockService.verifyBlock(chainTip);
            return "CHAIN_VALID=" + valid
                    + (valid ? " (tip=" + chainTip.getHash() + ", height="
                    + countBlocks(chainTip) + ")" : "");
        } catch (Exception e) {
            return "VERIFY_FAILED: " + e.getMessage();
        }
    }

    private int countBlocks(Block block) {
        int height = 0;
        while (block != null) {
            height++;
            block = block.getPreviousBlock();
        }
        return height;
    }
}
