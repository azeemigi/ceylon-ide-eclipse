package com.redhat.ceylon.eclipse.imp.editorActionContributions;

import org.eclipse.imp.editor.UniversalEditor;
import org.eclipse.imp.services.ILanguageActionsContributor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IFileEditorInput;

import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.eclipse.imp.core.CeylonReferenceResolver;
import com.redhat.ceylon.eclipse.imp.parser.CeylonParseController;
import com.redhat.ceylon.eclipse.imp.refactoring.RefactoringContributor;

public class CeylonEditorActionContributions implements
		ILanguageActionsContributor {

	private class FindReferencesAction extends Action {
		private final UniversalEditor editor;

		private FindReferencesAction(UniversalEditor editor) {
			super("Find References");
			this.editor = editor;
			setAccelerator(SWT.CONTROL | SWT.ALT | 'G');
		}

		@Override
		public void run() {
			CeylonParseController cpc = (CeylonParseController) editor.getParseController();
			Node node = cpc.getSourcePositionLocator().findNode(cpc.getRootNode(), 
					editor.getSelection().x, editor.getSelection().x+editor.getSelection().y);
			Declaration referencedDeclaration = CeylonReferenceResolver.getReferencedDeclaration(node);
			NewSearchUI.runQueryInBackground(new FindReferencesSearchQuery(cpc, referencedDeclaration, 
					((IFileEditorInput) editor.getEditorInput()).getFile()));
		}
	}

	public void contributeToEditorMenu(final UniversalEditor editor,
			IMenuManager menuManager) {
		//IMenuManager languageMenu = new MenuManager("Search");
		menuManager.add(new FindReferencesAction(editor));
	}

	public void contributeToMenuBar(UniversalEditor editor, IMenuManager menu) {
		//languageMenu = new MenuManager("ceylon");
		IMenuManager refactor = /*editor.getEditorSite().getActionBars()
				.getMenuManager()*/menu.findMenuUsingPath("refactorMenuId");
		if (refactor.getItems().length==0) {
			for (IAction action: RefactoringContributor.getActions(editor)) {
				refactor.add(action);
			}
		}
		IMenuManager search = /*editor.getEditorSite().getActionBars()
				.getMenuManager()*/menu.findMenuUsingPath("navigate");
		search.add(new Separator());
		search.add(new FindReferencesAction(editor));

	}

	public void contributeToStatusLine(final UniversalEditor editor,
			IStatusLineManager statusLineManager) {
		// TODO add ControlContribution objects to the statusLineManager
	}

	public void contributeToToolBar(UniversalEditor editor,
			IToolBarManager toolbarManager) {
		// add ControlContribution objects to the toolbarManager
	}
}