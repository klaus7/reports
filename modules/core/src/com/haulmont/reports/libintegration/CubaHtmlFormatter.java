/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

/**
 *
 * @author degtyarjov
 * @version $Id$
 */
package com.haulmont.reports.libintegration;

import com.haulmont.cuba.core.app.DataWorker;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.reports.ReportingConfig;
import com.haulmont.yarg.exception.ReportFormattingException;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.formatters.impl.HtmlFormatter;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.resource.ImageResource;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.UUID;

public class CubaHtmlFormatter extends HtmlFormatter {
    protected static final String CUBA_FONTS_DIR = "/cuba/fonts";

    public static final String FS_PROTOCOL_PREFIX = "fs://";

    protected Log log = LogFactory.getLog(getClass());

    public CubaHtmlFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
    }


    //todo degtyarjov, artamonov - get rid of custom processing of file descriptors, use field formats
    // we can append <content> with Base64 to html and put reference to <img> for html
    // and some custom reference if we need pdf and then implement ResourcesITextUserAgentCallback which will
    // take base64 from appropriate content
    protected void renderPdfDocument(String htmlContent, OutputStream outputStream) {
        ITextRenderer renderer = new ITextRenderer();
        try {
            File tmpFile = File.createTempFile("htmlReport", ".htm");
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(tmpFile));
            dataOutputStream.write(htmlContent.getBytes(Charset.forName("UTF-8")));
            dataOutputStream.close();

            loadFonts(renderer);

            String url = tmpFile.toURI().toURL().toString();
            renderer.setDocument(url);

            ResourcesITextUserAgentCallback userAgentCallback =
                    new ResourcesITextUserAgentCallback(renderer.getOutputDevice());
            userAgentCallback.setSharedContext(renderer.getSharedContext());

            renderer.getSharedContext().setUserAgentCallback(userAgentCallback);

            renderer.layout();
            renderer.createPDF(outputStream);

            FileUtils.deleteQuietly(tmpFile);
        } catch (Exception e) {
            throw wrapWithReportingException("", e);
        }
    }

    protected void loadFonts(ITextRenderer renderer) {
        Configuration configuration = AppBeans.get(Configuration.class);
        GlobalConfig config = configuration.getConfig(GlobalConfig.class);
        String fontsPath = config.getConfDir() + CUBA_FONTS_DIR;

        File fontsDir = new File(fontsPath);

        loadFontsFromDirectory(renderer, fontsDir);

        ReportingConfig serverConfig = configuration.getConfig(ReportingConfig.class);
        if (StringUtils.isNotBlank(serverConfig.getPdfFontsDirectory())) {
            File systemFontsDir = new File(serverConfig.getPdfFontsDirectory());
            loadFontsFromDirectory(renderer, systemFontsDir);
        }
    }

    protected void loadFontsFromDirectory(ITextRenderer renderer, File fontsDir) {
        if (fontsDir.exists()) {
            if (fontsDir.isDirectory()) {
                File[] files = fontsDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        String lower = name.toLowerCase();
                        return lower.endsWith(".otf") || lower.endsWith(".ttf");
                    }
                });
                for (File file : files) {
                    try {
                        // Usage of some fonts may be not permitted
                        renderer.getFontResolver().addFont(file.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                    } catch (IOException e) {
                        log.warn(e.getMessage());
                    } catch (DocumentException e) {
                        e.printStackTrace();
                    }
                }
            } else
                log.warn(String.format("File %s is not a directory", fontsDir.getAbsolutePath()));
        } else {
            log.debug("Fonts directory does not exist: " + fontsDir.getPath());
        }
    }

    protected class ResourcesITextUserAgentCallback extends ITextUserAgent {

        public ResourcesITextUserAgentCallback(ITextOutputDevice outputDevice) {
            super(outputDevice);
        }

        @Override
        public ImageResource getImageResource(String uri) {
            if (StringUtils.startsWith(uri, FS_PROTOCOL_PREFIX)) {
                ImageResource resource;
                resource = (ImageResource) _imageCache.get(uri);
                if (resource == null) {
                    InputStream is = resolveAndOpenStream(uri);
                    if (is != null) {
                        try {
                            Image image = Image.getInstance(IOUtils.toByteArray(is));

                            scaleToOutputResolution(image);
                            resource = new ImageResource(new ITextFSImage(image));
                            //noinspection unchecked
                            _imageCache.put(uri, resource);
                        } catch (Exception e) {
                            throw wrapWithReportingException(
                                    String.format("Can't read image file; unexpected problem for URI '%s'", uri), e);
                        } finally {
                            IOUtils.closeQuietly(is);
                        }
                    }
                }

                if (resource != null) {
                    ITextFSImage image = (ITextFSImage) resource.getImage();

                    com.lowagie.text.Image imageObject;
                    // use reflection for access to internal image
                    try {
                        Field imagePrivateField = image.getClass().getDeclaredField("_image");
                        imagePrivateField.setAccessible(true);

                        imageObject = (Image) imagePrivateField.get(image);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new ReportFormattingException("Error while clone internal image in Itext");
                    }

                    resource = new ImageResource(new ITextFSImage(imageObject));
                } else {
                    resource = new ImageResource(null);
                }

                return resource;
            }

            return super.getImageResource(uri);
        }

        protected void scaleToOutputResolution(Image image) {
            float factor = getSharedContext().getDotsPerPixel();
            image.scaleAbsolute(image.getPlainWidth() * factor, image.getPlainHeight() * factor);
        }

        @Override
        protected InputStream resolveAndOpenStream(String uri) {
            if (StringUtils.startsWith(uri, FS_PROTOCOL_PREFIX)) {
                String uuidString = StringUtils.substring(uri, FS_PROTOCOL_PREFIX.length());

                DataWorker dataWorker = AppBeans.get(DataWorker.class);
                LoadContext loadContext = new LoadContext(FileDescriptor.class);
                loadContext.setView(View.LOCAL);

                UUID id = UUID.fromString(uuidString);
                loadContext.setId(id);

                FileDescriptor fd = dataWorker.load(loadContext);
                if (fd == null) {
                    throw new ReportFormattingException(
                            String.format("File with id [%s] has not been found in file storage", id));
                }

                FileStorageAPI storageAPI = AppBeans.get(FileStorageAPI.class);
                try {
                    return storageAPI.openStream(fd);
                } catch (FileStorageException e) {
                    throw wrapWithReportingException(
                            String.format("An error occurred while loading file with id [%s] from file storage", id), e);
                }
            }

            return super.resolveAndOpenStream(uri);
        }
    }
}