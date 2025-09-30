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

import net.minecraft.network.chat.Component;

public class DurationArgument implements ArgumentType<Long> {
    
    private static final DurationArgument INSTANCE = new DurationArgument();
    
    public static final long SECOND_FACTOR = 1000;
    public static final long MINUTE_FACTOR = SECOND_FACTOR * 60;
    public static final long HOUR_FACTOR = MINUTE_FACTOR * 60;
    public static final long DAY_FACTOR = HOUR_FACTOR * 24;
    
    public static final List<String> EXAMPLES = Arrays.asList(new String[] { "10s", "30s", "1m", "500ms" });
    
    public static String printDuration(long duration) {
        StringBuilder output = new StringBuilder();
        long days = duration / DAY_FACTOR;
        if (days > 0) {
            output.append(" " + days + "d");
            duration -= days * DAY_FACTOR;
        }
        
        long hours = duration / HOUR_FACTOR;
        if (hours > 0) {
            output.append(" " + hours + "h");
            duration -= hours * HOUR_FACTOR;
        }
        
        long minutes = duration / MINUTE_FACTOR;
        if (minutes > 0) {
            output.append(" " + minutes + "m");
            duration -= minutes * MINUTE_FACTOR;
        }
        
        long seconds = duration / SECOND_FACTOR;
        if (seconds > 0) {
            output.append(" " + seconds + "s");
            duration -= seconds * SECOND_FACTOR;
        }
        
        if (duration > 0)
            output.append(" " + duration + "ms");
        
        return output.substring(1); // Remove first space
    }
    
    public static DurationArgument duration() {
        return new DurationArgument();
    }
    
    public static long getDuration(final CommandContext<?> context, final String name) {
        return context.getArgument(name, long.class);
    }
    
    public static long parseDuration(String value, long defaultValue) {
        try {
            return INSTANCE.parse(new StringReader(value));
        } catch (CommandSyntaxException e) {
            return defaultValue;
        }
    }
    
    @Override
    public Long parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        long time = reader.readLong();
        String type = reader.readString();
        long factor = 0;
        if (type.equalsIgnoreCase("ms"))
            factor = 1;
        else if (type.equalsIgnoreCase("s"))
            factor = SECOND_FACTOR;
        else if (type.equalsIgnoreCase("m"))
            factor = MINUTE_FACTOR;
        else if (type.equalsIgnoreCase("h"))
            factor = HOUR_FACTOR;
        else if (type.equalsIgnoreCase("d"))
            factor = DAY_FACTOR;
        else {
            reader.setCursor(start);
            throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Invalid time format try out 10s (for 10 seconds)")), Component.translatable(
                "invalid_time_format"));
        }
        
        return time * factor;
    }
    
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    
}
