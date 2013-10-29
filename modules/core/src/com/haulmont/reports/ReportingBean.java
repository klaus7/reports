/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.reports;

import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.reports.app.ParameterPrototype;
import com.haulmont.reports.entity.*;
import com.haulmont.reports.exception.FailedToLoadTemplateClassException;
import com.haulmont.reports.exception.ReportingException;
import com.haulmont.yarg.formatters.CustomReport;
import com.haulmont.yarg.reporting.ReportOutputDocument;
import com.haulmont.yarg.reporting.ReportingAPI;
import com.haulmont.yarg.reporting.RunParams;
import com.haulmont.yarg.structure.ReportOutputType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ExternalizableConverter;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang.StringUtils;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.CRC32;

/**
 * @author artamonov
 * @version $Id$
 */
@ManagedBean(ReportingApi.NAME)
public class ReportingBean implements ReportingApi {
    public static final String REPORT_EDIT_VIEW_NAME = "report.edit";

    protected PrototypesLoader prototypesLoader = new PrototypesLoader();

    @Inject
    protected Persistence persistence;

    @Inject
    protected Metadata metadata;

    @Inject
    protected FileStorageAPI fileStorageAPI;

    @Inject
    protected TimeSource timeSource;

    @Inject
    protected ReportingAPI reportingApi;

    @Inject
    protected Scripting scripting;

    @Inject
    protected ReportImportExportAPI reportImportExport;

    @Inject
    protected UuidSource uuidSource;

    @Override
    public ReportOutputDocument createReport(Report report, Map<String, Object> params) throws IOException {
        report = reloadEntity(report, REPORT_EDIT_VIEW_NAME);
        ReportTemplate reportTemplate = report.getDefaultTemplate();
        return createReportDocument(report, reportTemplate, params);
    }

    @Override
    public ReportOutputDocument createReport(Report report, String templateCode, Map<String, Object> params)
            throws IOException {
        report = reloadEntity(report, REPORT_EDIT_VIEW_NAME);
        ReportTemplate template = report.getTemplateByCode(templateCode);
        return createReportDocument(report, template, params);
    }

    @Override
    public ReportOutputDocument bulkPrint(Report report, List<Map<String, Object>> paramsList) {
        try {
            report = reloadEntity(report, REPORT_EDIT_VIEW_NAME);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(byteArrayOutputStream);
            zipOutputStream.setMethod(ZipArchiveOutputStream.STORED);
            zipOutputStream.setEncoding(ReportImportExport.ENCODING);

            ReportTemplate reportTemplate = report.getDefaultTemplate();
            Map<String, Integer> alreadyUsedNames = new HashMap<>();

            for (Map<String, Object> params : paramsList) {
                ReportOutputDocument reportDocument = createReportDocument(report, reportTemplate, params);

                String documentName = reportDocument.getDocumentName();
                if (alreadyUsedNames.containsKey(documentName)) {
                    int newCount = alreadyUsedNames.get(documentName) + 1;
                    alreadyUsedNames.put(documentName, newCount);
                    documentName = StringUtils.substringBeforeLast(documentName, ".") + newCount + "." + StringUtils.substringAfterLast(documentName, ".");
                    alreadyUsedNames.put(documentName, 1);
                } else {
                    alreadyUsedNames.put(documentName, 1);
                }

                ArchiveEntry singleReportEntry = newStoredEntry(documentName, reportDocument.getContent());
                zipOutputStream.putArchiveEntry(singleReportEntry);
                zipOutputStream.write(reportDocument.getContent());
            }

            zipOutputStream.closeArchiveEntry();
            zipOutputStream.close();


            ReportOutputDocument reportOutputDocument = new ReportOutputDocument(report, byteArrayOutputStream.toByteArray(), "Reports.zip", ReportOutputType.custom);
            return reportOutputDocument;
        } catch (IOException e) {
            throw new ReportingException("An error occurred while zipping report contents", e);
        }
    }

    private ArchiveEntry newStoredEntry(String name, byte[] data) {
        ZipArchiveEntry zipEntry = new ZipArchiveEntry(name);
        zipEntry.setSize(data.length);
        zipEntry.setCompressedSize(zipEntry.getSize());
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        zipEntry.setCrc(crc32.getValue());
        return zipEntry;
    }

    @Override
    public ReportOutputDocument createReport(Report report, ReportTemplate template, Map<String, Object> params)
            throws IOException {
        report = reloadEntity(report, REPORT_EDIT_VIEW_NAME);
        return createReportDocument(report, template, params);
    }

    private ReportOutputDocument createReportDocument(Report report, ReportTemplate template, Map<String, Object> params)
            throws IOException {
        try {
            List<String> prototypes = new LinkedList<>();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (param.getValue() instanceof ParameterPrototype)
                    prototypes.add(param.getKey());
            }
            Map<String, Object> resultParams = new HashMap<>(params);

            for (String paramName : prototypes) {
                ParameterPrototype prototype = (ParameterPrototype) params.get(paramName);
                List data = prototypesLoader.loadData(prototype);
                resultParams.put(paramName, data);
            }

            if (template.isCustom()) {
                Class<Object> reportClass = scripting.loadClass(template.getCustomClass());
                if (reportClass == null) {
                    throw new FailedToLoadTemplateClassException(template.getCustomClass());
                }
                template.setCustomReport((CustomReport) reportClass.newInstance());
            }

            return reportingApi.runReport(new RunParams(report).template(template).params(resultParams));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ReportingException(String.format("Could not instantiate class for custom template [%s]", template.getCustomClass()), e);
        }
    }

    public Report copyReport(Report source) {
        source = reloadEntity(source, REPORT_EDIT_VIEW_NAME);
        Report copiedReport = (Report) InstanceUtils.copy(source);

        copiedReport.setId(uuidSource.createUuid());
        copiedReport.setTemplates(null);

        List<ReportTemplate> copiedTemplates = new ArrayList<>();
        ReportTemplate defaultCopiedTemplate = null;

        for (ReportTemplate reportTemplate : source.getTemplates()) {
            ReportTemplate copiedTemplate = (ReportTemplate) InstanceUtils.copy(reportTemplate);
            copiedTemplate.setId(uuidSource.createUuid());
            copiedTemplate.setReport(copiedReport);
            copiedTemplates.add(copiedTemplate);
            if (source.getDefaultTemplate().equals(reportTemplate)) {
                defaultCopiedTemplate = copiedTemplate;
            }
        }


        Transaction tx = persistence.createTransaction();
        try {
            copiedReport.setName(generateReportName(source.getName(), 1));
            EntityManager em = persistence.getEntityManager();
            em.persist(copiedReport);
            for (ReportTemplate copiedTemplate : copiedTemplates) {
                em.persist(copiedTemplate);
            }
            copiedReport.setTemplates(copiedTemplates);
            copiedReport.setDefaultTemplate(defaultCopiedTemplate);

            copiedReport.setXml(convertToXml(copiedReport));
            tx.commit();
        } finally {
            tx.end();
        }

        return copiedReport;
    }

    private String generateReportName(String sourceName, int iteration) {
        EntityManager em = persistence.getEntityManager();
        String reportName = String.format("%s (%s)", sourceName, iteration);
        Query q = em.createQuery("select r from report$Report r where r.name = :name");
        q.setParameter("name", reportName);
        if (q.getResultList().size() > 0) {
            return generateReportName(sourceName, ++iteration);
        }
        return reportName;
    }


    @Override
    public byte[] exportReports(Collection<Report> reports) throws IOException, FileStorageException {
        return reportImportExport.exportReports(reports);
    }

    @Override
    public FileDescriptor createAndSaveReport(Report report,
                                              Map<String, Object> params, String fileName) throws IOException {
        report = reloadEntity(report, REPORT_EDIT_VIEW_NAME);
        ReportTemplate template = report.getDefaultTemplate();
        return createAndSaveReport(report, template, params, fileName);
    }

    @Override
    public FileDescriptor createAndSaveReport(Report report, String templateCode,
                                              Map<String, Object> params, String fileName) throws IOException {
        report = reloadEntity(report, REPORT_EDIT_VIEW_NAME);
        ReportTemplate template = report.getTemplateByCode(templateCode);
        return createAndSaveReport(report, template, params, fileName);
    }

    @Override
    public FileDescriptor createAndSaveReport(Report report, ReportTemplate template,
                                              Map<String, Object> params, String fileName) throws IOException {
        report = reloadEntity(report, REPORT_EDIT_VIEW_NAME);
        return createAndSaveReportDocument(report, template, params, fileName);
    }

    private FileDescriptor createAndSaveReportDocument(Report report, ReportTemplate template, Map<String, Object> params, String fileName) throws IOException {
        byte[] reportData = createReportDocument(report, template, params).getContent();
        String ext = template.getReportOutputType().toString().toLowerCase();

        return saveReport(reportData, fileName, ext);
    }

    private FileDescriptor saveReport(byte[] reportData, String fileName, String ext) throws IOException {
        FileDescriptor file = new FileDescriptor();
        file.setCreateDate(timeSource.currentTimestamp());
        file.setName(fileName + "." + ext);
        file.setExtension(ext);
        file.setSize(reportData.length);

        try {
            fileStorageAPI.saveFile(file, reportData);
        } catch (FileStorageException e) {
            throw new IOException(e);
        }

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            em.persist(file);
            tx.commit();
        } finally {
            tx.end();
        }
        return file;
    }

    @Override
    public Collection<Report> importReports(byte[] zipBytes) throws IOException, FileStorageException {
        return reportImportExport.importReports(zipBytes);
    }

    public String convertToXml(Report report) {
        XStream xStream = createXStream();
        String xml = xStream.toXML(report);
        return xml;
    }

    public Report convertToReport(String xml) {
        XStream xStream = createXStream();
        return (Report) xStream.fromXML(xml);
    }

    private static XStream createXStream() {
        XStream xStream = new XStream();
        xStream.getConverterRegistry().removeConverter(ExternalizableConverter.class);
        xStream.registerConverter(new CollectionConverter(xStream.getMapper()) {
            @Override
            public boolean canConvert(Class type) {
                return ArrayList.class.isAssignableFrom(type) ||
                        HashSet.class.isAssignableFrom(type) ||
                        LinkedList.class.isAssignableFrom(type) ||
                        LinkedHashSet.class.isAssignableFrom(type);

            }
        }, XStream.PRIORITY_VERY_HIGH);

        xStream.registerConverter(new DateConverter() {
            @Override
            public boolean canConvert(Class type) {
                return Date.class.isAssignableFrom(type);
            }
        });

        xStream.alias("report", Report.class);
        xStream.alias("band", BandDefinition.class);
        xStream.alias("dataSet", DataSet.class);
        xStream.alias("parameter", ReportInputParameter.class);
        xStream.alias("template", ReportTemplate.class);
        xStream.alias("screen", ReportScreen.class);
        xStream.alias("format", ReportValueFormat.class);
        xStream.aliasSystemAttribute(null, "class");
        xStream.omitField(ReportTemplate.class, "content");
        xStream.omitField(ReportInputParameter.class, "localeName");
        xStream.omitField(Report.class, "xml");

        return xStream;
    }


    private <T extends Entity> T reloadEntity(T entity, String viewName) {
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            View targetView = metadata.getViewRepository().getView(entity.getClass(), viewName);
            em.setView(targetView);
            entity = (T) em.find(entity.getClass(), entity.getId());
            if (entity != null) {
                em.setView(null);
                em.fetch(entity, targetView);
            }
            tx.commit();
            return entity;
        } finally {
            tx.end();
        }
    }
}