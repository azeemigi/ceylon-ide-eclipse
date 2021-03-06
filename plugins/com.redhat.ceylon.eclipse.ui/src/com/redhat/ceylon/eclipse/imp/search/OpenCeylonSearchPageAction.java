package com.redhat.ceylon.eclipse.imp.search;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.redhat.ceylon.eclipse.ui.CeylonPlugin;

public class OpenCeylonSearchPageAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null || window.getActivePage() == null) {
            Shell shell = CeylonPlugin.getInstance().getWorkbench()
                    .getActiveWorkbenchWindow().getShell();
            MessageDialog.openError(shell, "Ceylon Search Error", "No active window");
        }
        else {
            NewSearchUI.openSearchDialog(window, "com.redhat.ceylon.eclipse.ui.searchPage");
        }
        return null;
    }

}