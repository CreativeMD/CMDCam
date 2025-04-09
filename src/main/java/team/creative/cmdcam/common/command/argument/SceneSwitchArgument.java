package team.creative.cmdcam.common.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.StringReader;
import team.creative.cmdcam.client.CMDCamClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class SceneSwitchArgument implements ArgumentType<String> {

    public SceneSwitchArgument() {
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();
        if (input.equals("all") || input.equals("default")) {
            return input;
        }

        try {
            int value = Integer.parseInt(input);

            if (value >= 1 && value <= CMDCamClient.getScenesCount()) {
                return input;
            }
        } catch (NumberFormatException ignored) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, input);
        }

        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, input);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        int sceneCount = CMDCamClient.getScenesCount();
        builder.suggest("all");
        for (int i = 1; i <= sceneCount; i++) {
            builder.suggest(String.valueOf(i));
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("all", "1", String.valueOf(Math.max(1, CMDCamClient.getScenesCount())));
    }
}
