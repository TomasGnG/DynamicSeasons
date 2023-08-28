package de.tomasgng.utils.template;

import com.destroystokyo.paper.ParticleBuilder;
import de.tomasgng.DynamicSeasons;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Particles {

    @Setter
    private boolean enabled;
    @Setter
    private int offsetX;
    @Setter
    private int offsetY;
    @Setter
    private int offsetZ;
    @Setter
    private int spawnTime;
    @Setter
    private double speed;
    @Setter
    private Map<Particle, Integer[]> particleMap;
    private final Random random = new Random();

    public Particles(
            boolean enabled,
            int offsetX,
            int offsetY,
            int offsetZ,
            int spawnTime,
            double speed,
            Map<Particle, Integer[]> particleMap) {
        setEnabled(enabled);
        setOffsetX(offsetX);
        setOffsetY(offsetY);
        setOffsetZ(offsetZ);
        setSpawnTime(spawnTime);
        setSpeed(speed);
        setParticleMap(particleMap);
    }

    private boolean stopTimer = false;

    public void startParticleTimer() {
        if(!enabled)
            return;
        var worlds = DynamicSeasons.getInstance().getConfigManager().getAllowedWorlds();
        Bukkit.getScheduler().runTaskTimer(DynamicSeasons.getInstance(), task -> {
            if(stopTimer) {
                task.cancel();
                stopTimer = false;
                return;
            }
            List<Player> receivers = new ArrayList<>();
            worlds.forEach(world -> receivers.addAll(world.getPlayers()));
            for (var entry : particleMap.entrySet()) {
                var builder = new ParticleBuilder(entry.getKey())
                        .count(random.nextInt(entry.getValue()[0], entry.getValue()[1]+1))
                        .extra(speed)
                        .offset(offsetX, offsetY, offsetZ)
                        .force(false);
                receivers.forEach(player -> {
                    builder.location(player.getLocation()).spawn();
                });
            }
        }, 20L, spawnTime);
    }

    public void stopParticleTimer() {
        stopTimer = true;
    }

}
