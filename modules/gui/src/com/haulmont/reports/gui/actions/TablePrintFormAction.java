/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports.gui.actions;

import com.google.common.base.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.DialogAction.Type;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.reports.app.ParameterPrototype;
import com.haulmont.reports.gui.ReportGuiManager;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

public class TablePrintFormAction extends AbstractPrintFormAction {

    protected Window window;
    protected final Table table;

    /**
     * @deprecated Use {@link TablePrintFormAction#TablePrintFormAction(Table)} instead.
     */
    @Deprecated
    public TablePrintFormAction(Window window, Table table) {
        this("tableReport", window, table);
    }

    /**
     * @deprecated Use {@link TablePrintFormAction#TablePrintFormAction(String, Table)} instead.
     */
    @Deprecated
    public TablePrintFormAction(String id, Window window, Table table) {
        super(id);

        this.window = window;
        this.table = table;
        this.caption = messages.getMessage(TablePrintFormAction.class, "actions.Report");
        this.icon = "icons/reports-print.png";
    }

    public TablePrintFormAction(Table table) {
        this("tableReport", table);
    }

    public TablePrintFormAction(String id, Table table) {
        super(id);

        this.table = table;
        this.caption = messages.getMessage(TablePrintFormAction.class, "actions.Report");
        this.icon = "icons/reports-print.png";
    }

    @Override
    public void actionPerform(Component component) {
        DialogAction cancelAction = new DialogAction(Type.CANCEL);

        Window window = ComponentsHelper.getWindow(table);
        Preconditions.checkState(window != null, "Table is not attached to window");

        if (beforeActionPerformedHandler != null) {
            if (!beforeActionPerformedHandler.beforeActionPerformed())
                return;
        }

        final Set selected = table.getSelected();
        if (CollectionUtils.isNotEmpty(selected)) {
            if (selected.size() > 1) {
                Action printSelectedAction = new AbstractAction("actions.printSelected", Status.PRIMARY) {
                    @Override
                    public void actionPerform(Component component) {
                        printSelected(selected);
                    }
                };
                printSelectedAction.setIcon("icons/reports-print-row.png");

                Action printAllAction = new AbstractAction("actions.printAll") {
                    @Override
                    public void actionPerform(Component component) {
                        printAll();
                    }
                };
                printAllAction.setIcon("icons/reports-print-all.png");

                window.showOptionDialog(messages.getMessage(ReportGuiManager.class, "notifications.confirmPrintSelectedheader"),
                        messages.getMessage(ReportGuiManager.class, "notifications.confirmPrintSelected"),
                        Frame.MessageType.CONFIRMATION,
                        new Action[]{printAllAction, printSelectedAction, cancelAction});
            } else {
                printSelected(selected);
            }
        } else {
            CollectionDatasource ds = table.getDatasource();

            if ((ds.getState() == Datasource.State.VALID) && (ds.size() > 0)) {
                Action yesAction = new DialogAction(Type.OK) {
                    @Override
                    public void actionPerform(Component component) {
                        printAll();
                    }
                };

                cancelAction.setPrimary(true);

                window.showOptionDialog(messages.getMessage(TablePrintFormAction.class, "notifications.confirmPrintAllheader"),
                        messages.getMessage(TablePrintFormAction.class, "notifications.confirmPrintAll"),
                        Frame.MessageType.CONFIRMATION, new Action[]{yesAction, cancelAction});
            } else {
                window.showNotification(messages.getMessage(ReportGuiManager.class, "notifications.noSelectedEntity"),
                        Frame.NotificationType.HUMANIZED);
            }
        }
    }

    protected void printSelected(Set selected) {
        CollectionDatasource datasource = table.getDatasource();
        MetaClass metaClass = datasource.getMetaClass();
        openRunReportScreen(ComponentsHelper.getWindow(table), selected, metaClass);
    }

    protected void printAll() {
        CollectionDatasource datasource = table.getDatasource();

        MetaClass metaClass = datasource.getMetaClass();

        LoadContext loadContext = datasource.getCompiledLoadContext();

        ParameterPrototype parameterPrototype = new ParameterPrototype(metaClass.getName());
        parameterPrototype.setMetaClassName(metaClass.getName());
        LoadContext.Query query = loadContext.getQuery();
        parameterPrototype.setQueryString(query.getQueryString());
        parameterPrototype.setQueryParams(query.getParameters());
        parameterPrototype.setViewName(loadContext.getView().getName());
        parameterPrototype.setFirstResult(query.getFirstResult());
        parameterPrototype.setMaxResults(query.getMaxResults());

        openRunReportScreen(ComponentsHelper.getWindow(table), parameterPrototype, metaClass);
    }
}