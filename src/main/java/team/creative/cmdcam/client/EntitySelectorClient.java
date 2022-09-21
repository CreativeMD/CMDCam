package team.creative.cmdcam.client;

import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface EntitySelectorClient {
    
    public Entity findSingleEntityClient(CommandSourceStack source) throws CommandSyntaxException;
    
    public List<? extends Entity> findEntitiesClient(CommandSourceStack source) throws CommandSyntaxException;
    
    public Player findSinglePlayerClient(CommandSourceStack source) throws CommandSyntaxException;
    
    public List<Player> findPlayersClient(CommandSourceStack source) throws CommandSyntaxException;
    
}
