package net.chilicat.felixscr.intellij.jps;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.incremental.BuilderService;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;

public class Builder extends BuilderService {
    @NotNull
    @Override
    public List<? extends ModuleLevelBuilder> createModuleLevelBuilders() {
        return Collections.singletonList(new FelixModuleLevelBuilder());
    }
}
