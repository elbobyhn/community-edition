/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.scripts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.alfresco.util.StringBuilderWriter;
import org.alfresco.web.framework.model.TemplateInstance;
import org.alfresco.web.site.RenderUtil;
import org.alfresco.web.site.RequestContext;
import org.alfresco.web.site.exception.RendererExecutionException;
import org.alfresco.web.site.renderer.AbstractRenderer;
import org.alfresco.web.site.renderer.RendererContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Implementation of a renderer that executes a Freemarker template.
 * 
 * @author muzquiano
 * @author kevinr
 */
public class FreemarkerRenderer extends AbstractRenderer
{
    private static final String SCRIPT_RESULTS = "freemarkerRendererScriptResults";
    private PresentationTemplateProcessor templateProcessor;
    private PresentationScriptProcessor scriptProcessor;
    private Store templateStore;
    
    /**
     * One time init for the renderer instance
     */
    public void init(RendererContext rendererContext)
    {
        // retrieve the various Spring beans that are required for the renderer
        ServletContext servletContext = rendererContext.getRequest().getSession().getServletContext();
        ApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        RequestContext context = rendererContext.getRequestContext();
        
        // get the template processor
        String templateProcessorId = context.getConfig().getRendererDescriptor(getRendererType()).getStringProperty("template-processor-bean");
        if(templateProcessorId == null || templateProcessorId.length() == 0)
        {
            templateProcessorId = "webframework.templateprocessor";
        }
        templateProcessor = (PresentationTemplateProcessor)appContext.getBean(templateProcessorId);
        
        // get the script processor
        String scriptProcessorId = context.getConfig().getRendererDescriptor(getRendererType()).getStringProperty("script-processor-bean");
        if(scriptProcessorId == null || scriptProcessorId.length() == 0)
        {
            scriptProcessorId = "webframework.scriptprocessor";
        }
        scriptProcessor = (PresentationScriptProcessor)appContext.getBean(scriptProcessorId);
        
        // get and init template store
        templateStore = (Store)appContext.getBean("webframework.store.templates");
        templateStore.init();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.site.renderer.AbstractRenderer#head(org.alfresco.web.site.renderer.RendererContext)
     */
    public String head(RendererContext rendererContext)
        throws RendererExecutionException
    {
        RequestContext context = rendererContext.getRequestContext();
        
        // get the renderer destination property
        String uri = this.getRenderer();
        
        // the current format
        String format = context.getFormatId();
        
        /**
         * Attempt to execute the templates associated .head. file, if it has one
         */
        String head = null;
        String templateName = null;
        try
        {
            // path to the template (switches on format)
            templateName = uri + ((format != null && format.length() != 0 &&
                    !context.getConfig().getDefaultFormatId().equals(format)) ? ("." + format + ".head.ftl") : ".head.ftl");
            
            if (templateProcessor.hasTemplate(templateName))
            {
                // build the model
                Map<String, Object> model = new HashMap<String, Object>(32);
                ProcessorModelHelper.populateTemplateModel(rendererContext, model);
                
                StringBuilderWriter out = new StringBuilderWriter(512);
                templateProcessor.process(templateName, model, out);
                
                head = out.toString();
            }
        }
        catch (Exception ex) 
        {   
            throw new RendererExecutionException("FreemarkerRenderer failed to process template: " + templateName, ex);
        }
        
        return head;
    }
 
    /**
     * Execute.
     * 
     * @param rendererContext the renderer context
     * 
     * @throws RendererExecutionException the renderer execution exception
     */
    public void execute(RendererContext rendererContext)
            throws RendererExecutionException
    {
        RequestContext context = rendererContext.getRequestContext();
        
        // get the renderer destination property
        String uri = this.getRenderer();
        
        // the current format
        String format = context.getFormatId();
        
        // Now execute the real template
        String templateName = null;
        try
        {
            // the result model
            Map<String, Object> resultModel = null;
            
            if (rendererContext.getObject() instanceof TemplateInstance)
            {
                if (context.hasValue(SCRIPT_RESULTS) == false)
                {
                    // Attempt to execute a .js file for this page template
                    resultModel = new HashMap<String, Object>(8, 1.0f);
                    ScriptContent script = templateStore.getScriptLoader().getScript(uri + ".js");
                    if (script != null)
                    {
                        // build the model
                        Map<String, Object> scriptModel = new HashMap<String, Object>(8);
                        ProcessorModelHelper.populateScriptModel(rendererContext, scriptModel);
                        
                        // add in the model object
                        scriptModel.put("model", resultModel);
                        
                        // execute the script
                        scriptProcessor.executeScript(script, scriptModel);
                    }
                    
                    // store the result model in the request context for the next pass
                    // this removes the need to execute the script twice
                    if (context.hasValue(RenderUtil.PASSIVE_MODE_MARKER))
                    {
                        context.setValue(SCRIPT_RESULTS, (Serializable)resultModel);
                    }
                }
                else
                {
                    // retrieve results from the request context - we already executed a pass
                    resultModel = (Map<String, Object>)context.getValue(SCRIPT_RESULTS);
                    
                    // remove the results from the context - we do not want other templates finding it
                    context.removeValue(SCRIPT_RESULTS);
                }
            }
            
            // Execute the template file itself
            Map<String, Object> templateModel = new HashMap<String, Object>(32);
            ProcessorModelHelper.populateTemplateModel(rendererContext, templateModel);
            
            // merge script results model into the template model
            // these may not exist if a .js file was not found
            if (resultModel != null)
            {
                for (Map.Entry<String, Object> entry : resultModel.entrySet())
                {
                    // retrieve script model value and unwrap each java object from script object
                    Object value = entry.getValue();
                    Object templateValue = scriptProcessor.unwrapValue(value);
                    templateModel.put(entry.getKey(), templateValue);
                }
            }
            
            // path to the template (switches on format)
            templateName = uri + ".ftl";
            if (format != null && format.length() != 0 && !context.getConfig().getDefaultFormatId().equals(format))
            {
                templateName = uri + "." + format + ".ftl";
            }
            
            // process the template
            templateProcessor.process(templateName, templateModel, rendererContext.getResponse().getWriter());
        }
        catch(Exception ex)
        {
            throw new RendererExecutionException("FreemarkerRenderer failed to process template: " + templateName, ex);
        }
    }
}
