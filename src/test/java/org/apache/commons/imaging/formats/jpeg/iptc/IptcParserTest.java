/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.imaging.formats.jpeg.iptc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.formats.jpeg.JpegPhotoshopMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {#link {@link IptcParser} class.
 * @since 1.0-alpha2
 */
public class IptcParserTest {

    /**
     * Some block types (or Image Resource Blocks in Photoshop specification) have a recommendation
     * to not be interpreted by parsers, as they are handled by Photoshop in a special way,
     * that varies by platform (e.g. Mac, Windows, etc).
     *
     * IMAGING-246 provided a test image with APP13 segments, with some of the block types,
     * such as 1084. When the IptcParser is reading this block, the 4-bytes length record
     * may give a value that is larger than the block size.
     *
     * When such a case happens, the IptcParser fails with an exception, to avoid a memory
     * error. To prevent that, the parser must be able to skip these blocks that the specification
     * says "It is recommened that you do not interpret or use this data".
     *
     * @throws IOException when reading input
     * @throws ImageReadException when parsing file
     * @throws NoSuchAlgorithmException 
     *
     * @see https://www.adobe.com/devnet-apps/photoshop/fileformatashtml/
     */
    @Test
    public void testSkipBlockTypes() throws ImageReadException, IOException, NoSuchAlgorithmException {
        final String location = IptcParserTest.class
                .getResource("/images/jpeg/photoshop/IMAGING-246/FallHarvestKitKat_07610.jpg")
                .getFile();
        final File imageFile = new File(location);
        final JpegImageMetadata metadata = (JpegImageMetadata) new JpegImageParser()
                .getMetadata(new ByteSourceFile(imageFile), new HashMap<>());
        final JpegPhotoshopMetadata photoshopMetadata = metadata.getPhotoshop();
        final PhotoshopApp13Data photoshopApp13Data = photoshopMetadata.photoshopApp13Data;
        final List<IptcBlock> blocks = photoshopApp13Data.getRawBlocks();
        assertEquals(2, blocks.size());
        for (IptcBlock block : blocks) {
            if (block.getBlockType() == 1028) {
                // 0x0404 IPTC-NAA record
                byte[] data = block.getBlockData();
                assertTrue(data.length > 0);
            } else if (block.getBlockType() == 1061) {
                // 0x0425 (Photoshop 7.0) Caption digest
                byte[] data = block.getBlockData();
                assertTrue(data.length > 0);
            } else {
                fail("Unexpected block type found: " + block.getBlockType());
            }
        }
    }
}
