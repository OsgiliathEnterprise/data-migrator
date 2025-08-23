package net.osgiliath.migrator.core.processing.utils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamSlicer {
    public static <T> Stream<T>
    getSliceOfStream(Stream<T> stream, int startIndex, int endIndex) {
        return stream
                // Convert the stream to list
                .collect(Collectors.toList())

                // Fetch the subList between the specified index
                .subList(startIndex, endIndex + 1)

                // Convert the subList to stream
                .stream();
    }
}
