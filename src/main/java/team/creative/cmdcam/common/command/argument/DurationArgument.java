package team.creative.cmdcam.common.command.argument;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.network.chat.TranslatableComponent;

public class DurationArgument implements ArgumentType<Long> {
    
    public static final List<String> EXAMPLES = Arrays.asList(new String[] { "10s", "30s", "1m", "500ms" });
    
    public static DurationArgument duration() {
        return new DurationArgument();
    }
    
    public static long getDuration(final CommandContext<?> context, final String name) {
        return context.getArgument(name, long.class);
    }
    
    @Override
    public Long parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        long time = reader.readLong();
        String type = reader.readString();
        int factor = 0;
        if (type.equalsIgnoreCase("ms"))
            factor = 1;
        else if (type.equalsIgnoreCase("s"))
            factor = 1000;
        else if (type.equalsIgnoreCase("m"))
            factor = 1000 * 60;
        else if (type.equalsIgnoreCase("h"))
            factor = 1000 * 60 * 60;
        else if (type.equalsIgnoreCase("d"))
            factor = 1000 * 60 * 60 * 24;
        else {
            reader.setCursor(start);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Invalid time format")), new TranslatableComponent("invalid_mode"));
        }
        
        return time * factor;
    }
    
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    
}
