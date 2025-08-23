package net.osgiliath.migrator.core.processing.utils;

/*-
 * #%L
 * datamigrator-core
 * %%
 * Copyright (C) 2024 - 2025 Osgiliath Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
