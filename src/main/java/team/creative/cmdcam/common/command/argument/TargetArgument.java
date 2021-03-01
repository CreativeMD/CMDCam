package team.creative.cmdcam.common.command.argument;

import java.util.Arrays;
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

import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;

public class TargetArgument implements ArgumentType<String> {
    
    public static final List<String> EXAMPLES = Arrays.asList("self", "none");
    
    public static TargetArgument target() {
        return new TargetArgument();
    }
    
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String result = reader.readString();
        if (!result.equalsIgnoreCase("none") && !result.equalsIgnoreCase("self")) {
            reader.setCursor(start);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Invalid target")), new StringTextComponent("Invalid target!"));
        }
        
        return result;
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return context.getSource() instanceof ISuggestionProvider ? ISuggestionProvider.suggest(EXAMPLES, builder) : Suggestions.empty();
    }
    
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    
}
