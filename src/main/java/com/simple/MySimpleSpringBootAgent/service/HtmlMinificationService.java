package com.simple.MySimpleSpringBootAgent.service;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HtmlMinificationService {

    private final HtmlCompressor compressor;

    public HtmlMinificationService() {
        this.compressor = new HtmlCompressor();

        // Configure for maximum compression
        compressor.setRemoveComments(true);
        compressor.setRemoveMultiSpaces(true);
        compressor.setRemoveIntertagSpaces(true);
        compressor.setRemoveQuotes(false); // Keep quotes for safety
        compressor.setCompressCss(true);
        compressor.setCompressJavaScript(false); // Don't compress JS (shouldn't have any)
        compressor.setSimpleDoctype(true);
        compressor.setRemoveScriptAttributes(true);
        compressor.setRemoveStyleAttributes(true);
        compressor.setRemoveLinkAttributes(true);
        compressor.setRemoveFormAttributes(true);
        compressor.setRemoveInputAttributes(true);
        compressor.setSimpleBooleanAttributes(true);
        compressor.setRemoveJavaScriptProtocol(true);
        compressor.setRemoveHttpProtocol(false);
        compressor.setRemoveHttpsProtocol(false);
        compressor.setPreserveLineBreaks(false);
    }

    public String minify(String html) {
        if (html == null || html.isEmpty()) {
            log.warn("Empty HTML provided for minification");
            return html;
        }

        int originalSize = html.length();
        log.debug("Minifying HTML of size: {} bytes", originalSize);

        try {
            String minified = compressor.compress(html);
            int minifiedSize = minified.length();
            int reduction = originalSize - minifiedSize;
            double percentReduction = (reduction * 100.0) / originalSize;

            log.debug("Minification complete: {} → {} bytes ({} bytes reduced, {}% reduction)",
                    originalSize, minifiedSize, reduction, String.format("%.1f", percentReduction));

            return minified;
        } catch (Exception e) {
            log.error("Error during HTML minification: {}", e.getMessage());
            return html; // Return original on error
        }
    }
}
