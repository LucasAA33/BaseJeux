package basejeux.editor.dialog;

import basejeux.model.GameObjectDefinition;
import basejeux.model.event.Event;
import basejeux.model.event.EventType;

import javax.swing.*;
import java.awt.*;

public class InteractEventDialog extends JDialog {

    private boolean validated = false;

    public InteractEventDialog(JFrame parent, Event event, GameObjectDefinition def)
 {
        super(parent, "Configurer événement Interact", true);
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));

        JComboBox<String> animationBox = new JComboBox<>();
        
        if (def.animations != null && def.animations.list != null) {
            for (String animKey : def.animations.list.keySet()) {
                animationBox.addItem(animKey);
            }
        }

        JTextField requireItemField = new JTextField(
                event.requireItem != null ? event.requireItem : ""
        );

        JCheckBox teleportCheck = new JCheckBox("Téléporter après animation");

        JTextField mapField = new JTextField();
        JTextField spawnXField = new JTextField();
        JTextField spawnYField = new JTextField();

        JPanel teleportPanel = new JPanel(new GridLayout(0, 2, 4, 4));
        teleportPanel.add(new JLabel("Map cible"));
        teleportPanel.add(mapField);
        teleportPanel.add(new JLabel("Spawn X"));
        teleportPanel.add(spawnXField);
        teleportPanel.add(new JLabel("Spawn Y"));
        teleportPanel.add(spawnYField);
        teleportPanel.setVisible(false);

        teleportCheck.addActionListener(e ->
                teleportPanel.setVisible(teleportCheck.isSelected())
        );

        form.add(new JLabel("Animation"));
        form.add(animationBox);

        form.add(new JLabel("Item requis"));
        form.add(requireItemField);

        form.add(teleportCheck);
        form.add(new JLabel(""));

        add(form, BorderLayout.NORTH);
        add(teleportPanel, BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Annuler");

        ok.addActionListener(e -> {
            event.type = EventType.INTERACT;
            event.animation = (String) animationBox.getSelectedItem();

            if (event.animation != null) {
                java.util.Map<String, Object> autoState = new java.util.HashMap<>();
                autoState.put(event.animation, true);
                event.setState = autoState;
            }

            event.requireItem = requireItemField.getText().trim().isEmpty()
                    ? null : requireItemField.getText().trim();

            if (teleportCheck.isSelected()) {
                Event end = new Event();
                end.type = EventType.TELEPORT;
                end.teleportTo = mapField.getText().trim();
                end.spawnX = parseInt(spawnXField.getText());
                end.spawnY = parseInt(spawnYField.getText());
                event.onAnimationEnd = end;
            }

            validated = true;
            dispose();
        });

        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.add(ok);
        buttons.add(cancel);

        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private Integer parseInt(String txt) {
        try { return Integer.parseInt(txt.trim()); }
        catch (Exception e) { return null; }
    }

    public boolean isValidated() {
        return validated;
    }
}
