package net.chilicat.felixscr.intellij.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dkuffner
 */
public class SettingsPage {
    private JCheckBox enabledBox;
    private JCheckBox strictModeBox;
    private JComboBox specBox;
    private JPanel page;
    private JComboBox manifestPolicyBox;
    private JCheckBox generateAccessorsBox;
    private JCheckBox optimizedBuildBox;
    private boolean modified = false;

    public SettingsPage() {
        createUIComponents();
    }

    public JComponent getComponent() {
        return page;
    }

    private void createUIComponents() {

        final List<ItemSelectable> list = new ArrayList<ItemSelectable>();
        list.add(enabledBox);
        list.add(generateAccessorsBox);
        list.add(strictModeBox);
        list.add(specBox);
        list.add(manifestPolicyBox);
        list.add(optimizedBuildBox);

        ModifyUpdateListener l = new ModifyUpdateListener();
        for (ItemSelectable s : list) {
            s.addItemListener(l);
        }

        enabledBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                boolean enabled = enabledBox.isSelected();
                for (ItemSelectable s : list) {
                    if (s != enabledBox) {
                        ((JComponent) s).setEnabled(enabled);
                    }
                }
            }
        });

    }

    public boolean isModified() {
        return modified;
    }

    public void apply(ScrSettings settingsState) {
        settingsState.setEnabled(enabledBox.isSelected());
        settingsState.setStrictMode(strictModeBox.isSelected());
        settingsState.setSpec(specBox.getSelectedItem().toString());
        settingsState.setGenerateAccessors(generateAccessorsBox.isSelected());
        settingsState.setOptimizedBuild(optimizedBuildBox.isSelected());

        ManifestPolicy manifestPolicy = ManifestPolicy.valueOf(manifestPolicyBox.getSelectedItem().toString());
        settingsState.setManifestPolicy(manifestPolicy);
        modified = false;
    }

    public void reset(ScrSettings settingsState) {
        enabledBox.setSelected(settingsState.isEnabled());
        strictModeBox.setSelected(settingsState.isStrictMode());
        specBox.setSelectedItem(settingsState.getSpec());
        manifestPolicyBox.setSelectedItem(settingsState.getManifestPolicy().name());
        generateAccessorsBox.setSelected(settingsState.isGenerateAccessors());
        optimizedBuildBox.setSelected(settingsState.isOptimizedBuild());
        modified = false;
    }

    private class ModifyUpdateListener implements ItemListener {
        public void itemStateChanged(ItemEvent itemEvent) {
            modified = true;
        }
    }
}
