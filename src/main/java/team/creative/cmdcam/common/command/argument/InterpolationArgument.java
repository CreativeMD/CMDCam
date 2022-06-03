package team.creative.cmdcam.common.command.argument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import net.minecraft.network.chat.TranslatableComponent;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;

public class InterpolationArgument implements ArgumentType<String> {
    
    public static InterpolationArgument interpolation() {
        return new InterpolationArgument();
    }
    
    public static AllInterpolationArgument interpolationAll() {
        return new AllInterpolationArgument();
    }
    
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String result = reader.readString();
        if (!isAllowed(result)) {
            reader.setCursor(start);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Invalid interpolation")), new TranslatableComponent("invalid_mode"));
        }
        
        return result;
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider ? SharedSuggestionProvider.suggest(getAll(), builder) : Suggestions.empty();
    }
    
    @Override
    public Collection<String> getExamples() {
        return getAll();
    }
    
    public Collection<String> getAll() {
        return CamInterpolation.REGISTRY.keys();
    }
    
    public boolean isAllowed(String result) {
        return CamInterpolation.REGISTRY.get(result) != null;
    }
    
    public static class AllInterpolationArgument extends InterpolationArgument {
        
        public static final List<String> EXAMPLES;
        
        @Override
        public boolean isAllowed(String result) {
            return result.equalsIgnoreCase("all") || super.isAllowed(result);
        }
        
        @Override
        public Collection<String> getAll() {
            return EXAMPLES;
        }
        
        static {
            EXAMPLES = new ArrayList<>();
            EXAMPLES.add("all");
            EXAMPLES.addAll(CamInterpolation.REGISTRY.keys());
        }
        
    }
    
}
