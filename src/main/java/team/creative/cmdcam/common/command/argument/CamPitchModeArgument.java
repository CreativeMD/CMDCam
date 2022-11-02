package team.creative.cmdcam.common.command.argument;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import team.creative.cmdcam.common.math.interpolation.CamPitchMode;
import team.creative.cmdcam.common.scene.mode.CamMode;

public class CamPitchModeArgument implements ArgumentType<CamPitchMode> {
    
    public static CamPitchModeArgument pitchMode() {
        return new CamPitchModeArgument();
    }
    
    public static CamPitchMode getMode(final CommandContext<?> context, final String name) {
        return context.getArgument(name, CamPitchMode.class);
    }
    
    @Override
    public CamPitchMode parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String result = reader.readString();
        try {
            return CamPitchMode.of(result);
        } catch (IllegalArgumentException e) {
            reader.setCursor(start);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Invalid mode")), Component.translatable("invalid_mode"));
        }
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider ? SharedSuggestionProvider.suggest(CamPitchMode.NAMES, builder) : Suggestions.empty();
    }
    
    @Override
    public Collection<String> getExamples() {
        return CamMode.REGISTRY.keys();
    }
    
}
