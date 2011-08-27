package com.redhat.ceylon.eclipse.imp.refactoring;

import java.util.Iterator;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.imp.services.IASTFindReplaceTarget;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.ui.FindDeclarationVisitor;
import com.redhat.ceylon.compiler.typechecker.ui.FindReferenceVisitor;
import com.redhat.ceylon.eclipse.imp.occurrenceMarker.CeylonOccurrenceMarker;
import com.redhat.ceylon.eclipse.imp.parser.CeylonParseController;
import com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator;

public class InlineRefactoring extends Refactoring {
	private final IFile fSourceFile;
	private final Node fNode;
	private final ITextEditor fEditor;
	private final CeylonParseController parseController;
	private final Declaration dec;
	private boolean delete = true;
	private final int count;

	public InlineRefactoring(ITextEditor editor) {

		fEditor = editor;

		IASTFindReplaceTarget frt = (IASTFindReplaceTarget) fEditor;
		IEditorInput input = editor.getEditorInput();
		parseController = (CeylonParseController) frt.getParseController();

		if (input instanceof IFileEditorInput) {
			IFileEditorInput fileInput = (IFileEditorInput) input;
			fSourceFile = fileInput.getFile();
			fNode = findNode(frt);
			dec = CeylonOccurrenceMarker.getDeclaration(fNode);
			FindReferenceVisitor frv = new FindReferenceVisitor(dec);
			parseController.getRootNode().visit(frv);
			count = frv.getNodes().size();
			
		} 
		else {
			fSourceFile = null;
			fNode = null;
			dec = null;
			count = 0;
		}
	}
	
	public int getCount() {
		return count;
	}

	private Node findNode(IASTFindReplaceTarget frt) {
		return parseController.getSourcePositionLocator()
				.findNode(parseController.getRootNode(), frt.getSelection().x, 
						frt.getSelection().x+frt.getSelection().y);
	}

	public String getName() {
		return "Inline value";
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// Check parameters retrieved from editor context
		return new RefactoringStatus();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		TextFileChange tfc = new TextFileChange("Inline value", fSourceFile);
		tfc.setEdit(new MultiTextEdit());
		if (dec!=null) {
			FindDeclarationVisitor fdv = new FindDeclarationVisitor(dec);
			parseController.getRootNode().visit(fdv);
			Tree.AttributeDeclaration att = (Tree.AttributeDeclaration) fdv.getDeclarationNode();
			Tree.Term t = att.getSpecifierOrInitializerExpression().getExpression().getTerm();
			Integer start = t.getStartIndex();
			int length = t.getStopIndex()-start+1;
			Region region = new Region(start, length);
			String exp = "";
			for (Iterator<Token> ti = parseController.getTokenIterator(region); ti.hasNext();) {
				exp+=ti.next().getText();
			}
			FindReferenceVisitor frv = new FindReferenceVisitor(dec);
			parseController.getRootNode().visit(frv);
			for (Node node: frv.getNodes()) {
				node = CeylonSourcePositionLocator.getIdentifyingNode(node);
				tfc.addEdit(new ReplaceEdit(node.getStartIndex(), 
						node.getText().length(), exp));	
			}
			if (delete) {
				CommonToken from = (CommonToken) att.getToken();
				Tree.AnnotationList anns = att.getAnnotationList();
				if (!anns.getAnnotations().isEmpty()) {
					from = (CommonToken) anns.getAnnotations().get(0).getToken();
				}
				int prevIndex = from.getTokenIndex()-1;
				if (prevIndex>=0) {
					CommonToken tok = (CommonToken) parseController.getTokenStream().get(prevIndex);
					if (tok.getChannel()==Token.HIDDEN_CHANNEL) {
						from=tok;
					}
				}
				tfc.addEdit(new DeleteEdit(from.getStartIndex(), att.getStopIndex()-from.getStartIndex()+1));
			}
		}
		return tfc;
	}

	public Declaration getDeclaration() {
		return dec;
	}
	
	public void setDelete() {
		this.delete = !delete;
	}
}
