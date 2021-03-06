package com.anjlab.eclipse.tapestry5;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

@SuppressWarnings("restriction")
public class EclipseUtils
{
    public static final String ECLIPSE_INTEGRATION_FOR_TAPESTRY5 = "Eclipse Integration for Tapestry5";
    
    public static final String SOURCE_NOT_FOUND = "source not found";

    public static ISelection getProjectExplorerSelection(IWorkbenchWindow window)
    {
        return window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
    }

    public static interface EditorCallback
    {
        void editorOpened(IEditorPart editorPart);
    }
    
    public static void openFile(final IWorkbenchWindow window, final IFile file)
    {
        openFile(window, file, null);
    }
    
    public static void openFile(final IWorkbenchWindow window, TapestryFile file)
    {
        openFile(window, file, null);
    }
    
    public static void openFile(final IWorkbenchWindow window, TapestryFile file, final EditorCallback editorCallback)
    {
        if (file instanceof AssetReference)
        {
            AssetReference asset = (AssetReference) file;
            try
            {
                file = asset.resolveFile(false);
            }
            catch (AssetException e)
            {
                EclipseUtils.openError(window,
                        "Unable to resolve asset '" + asset.getAssetPath() + "': "
                                + e.getLocalizedMessage());
                
                return;
            }
        }
        
        if (file instanceof LocalFile)
        {
            openFile(window, ((LocalFile) file).getFile(), editorCallback);
        }
        else if (file instanceof JarEntryFile)
        {
            openInEditor(((JarEntryFile) file).getJarEntry(), editorCallback);
        }
        else if (file instanceof ClassFile)
        {
            openInEditor(((ClassFile) file).getClassFile(), editorCallback);
        }
    }

    private static void openInEditor(Object inputElement, final EditorCallback editorCallback)
    {
        try
        {
            IEditorPart editorPart = EditorUtility.openInEditor(inputElement);
            
            if (editorCallback != null)
            {
                editorCallback.editorOpened(editorPart);
            }
        }
        catch (PartInitException e)
        {
            Activator.getDefault().logError("Unable to open editor", e);
        }
    }
    
    public static void openFile(final IWorkbenchWindow window, final IFile file, final EditorCallback editorCallback)
    {
        window.getShell().getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                try
                {
                    IEditorPart editor = IDE.openEditor(window.getActivePage(), file, true);
                    
                    if (editorCallback != null)
                    {
                        editorCallback.editorOpened(editor);
                    }
                }
                catch (Exception e)
                {
                    Activator.getDefault().logError("Unable to open editor", e);
                    
                    openError(window, "Unable to open editor: " + e.getLocalizedMessage());
                }
            }
        });
    }

    public static void openError(final IWorkbenchWindow window, String message)
    {
        MessageDialog.openError(
                window.getShell(),
                ECLIPSE_INTEGRATION_FOR_TAPESTRY5,
                message);
    }

    public static void openInformation(final IWorkbenchWindow window, String message)
    {
        MessageDialog.openInformation(
                window.getShell(),
                ECLIPSE_INTEGRATION_FOR_TAPESTRY5,
                message);
    }

    public static <T> List<T> getAllAffectedResources(IResourceDelta delta, Class<T> clazz)
    {
        return getAllAffectedResources(delta, clazz, 0xFFFFFFFF);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> List<T> getAllAffectedResources(IResourceDelta delta, Class<T> clazz, int deltaKind)
    {
        List<T> files = new ArrayList<T>();
        
        for (IResourceDelta child : delta.getAffectedChildren())
        {
            IResource resource = child.getResource();
            
            if (resource != null && clazz.isAssignableFrom(resource.getClass()))
            {
                if ((child.getKind() & deltaKind) != 0)
                {
                    files.add((T) resource);
                }
            }
            else
            {
                files.addAll(getAllAffectedResources(child, clazz, deltaKind));
            }
        }
        return files;
    }

    public static boolean isSourceFolder(IContainer container) throws JavaModelException
    {
        return EclipseUtils.isSourceFolder((IJavaElement) container.getAdapter(IJavaElement.class));
    }

    public static boolean isSourceFolder(IJavaElement javaElement) throws JavaModelException
    {
        return javaElement != null
            && (javaElement instanceof IPackageFragmentRoot)
            && (((IPackageFragmentRoot) javaElement).getKind() == IPackageFragmentRoot.K_SOURCE);
    }

    public static IFile findFileCaseInsensitive(IContainer container, String componentPath)
    {
        String[] parts = (componentPath.startsWith("/") ? componentPath.substring(1) : componentPath).split("/");
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            try
            {
                boolean found = false;
                
                for (IResource member : container.members())
                {
                    if (part.equalsIgnoreCase(member.getName()))
                    {
                        if (member instanceof IFile && i == parts.length - 1)
                        {
                            return (IFile) member;
                        }
                        
                        if (!(member instanceof IContainer))
                        {
                            return null;
                        }
                        
                        container = (IContainer) member;
                        
                        found = true;
                        
                        break;
                    }
                }
                
                if (!found)
                {
                    return null;
                }
            }
            catch (CoreException e)
            {
            }
        }
        return null;
    }

    public static IField findFieldDeclaration(IProject project, Name name)
    {
        SearchPattern pattern = SearchPattern.createPattern(name.getFullyQualifiedName(),
                IJavaSearchConstants.FIELD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_FULL_MATCH);
        
        final List<SearchMatch> matches = searchJava(project, pattern);
        
        return exactMatchOrNull(matches, IField.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T exactMatchOrNull(final List<SearchMatch> matches, Class<T> clazz)
    {
        for (SearchMatch match : matches)
        {
            if (match.isExact() && clazz.isAssignableFrom(match.getElement().getClass()))
            {
                return (T) match.getElement();
            }
        }
        
        return null;
    }

    public static IType findTypeDeclaration(IProject project, String moduleClassName)
    {
        SearchPattern pattern = SearchPattern.createPattern(moduleClassName,
                IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_FULL_MATCH);
        
        final List<SearchMatch> matches = searchJava(project, pattern);
        
        return exactMatchOrNull(matches, IType.class);
    }

    private static List<SearchMatch> searchJava(IProject project,
            SearchPattern pattern)
    {
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
                new IJavaElement[] { JavaCore.create(project) });
        
        final List<SearchMatch> matches = new ArrayList<SearchMatch>();
        
        SearchRequestor requestor = new SearchRequestor()
        {
            @Override
            public void acceptSearchMatch(SearchMatch match) throws CoreException
            {
                matches.add(match);
            }
        };
    
        SearchEngine searchEngine = new SearchEngine();
        
        try
        {
            searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
                                scope, requestor, null);
        }
        catch (CoreException e)
        {
            Activator.getDefault().logWarning("Error performing search", e);;
        }
        return matches;
    }

    public static CompilationUnit parse(ICompilationUnit unit)
    {
        String source;
        try
        {
            source = unit.getSource();
        }
        catch (JavaModelException e)
        {
            throw new IllegalStateException(SOURCE_NOT_FOUND, e);
        }
        
        return parse(source);
    }

    public static CompilationUnit parse(String source)
    {
        if (source == null)
        {
            throw new IllegalStateException(SOURCE_NOT_FOUND);
        }
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }

    public static IProject getProjectFromSelection(ISelection selection)
    {
        if (selection instanceof ITreeSelection)
        {
            Object firstElement = ((ITreeSelection) selection).getFirstElement();
            
            if (firstElement != null)
            {
                IResource resource = (IResource) Platform.getAdapterManager().getAdapter(
                        firstElement, IResource.class);
                
                if (resource != null)
                {
                    return resource.getProject();
                }
            }
        }
        
        return null;
    }

}
