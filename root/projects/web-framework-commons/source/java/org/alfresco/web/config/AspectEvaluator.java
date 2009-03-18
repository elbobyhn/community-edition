/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Evaluator that determines whether a given object has a particular aspect
 * applied.
 * 
 * @author Neil McErlean
 */
public class AspectEvaluator extends NodeMetadataBasedEvaluator
{
    static Log logger = LogFactory.getLog(AspectEvaluator.class);

    @Override
    protected Log getLogger()
    {
        return logger;
    }

    /**
     * This method checks if the specified condition is matched by any of the aspects
     * within the specified jsonResponse String.
     * 
     * @return true if any aspect matches the condition, else false.
     */
    @Override
    protected boolean checkJsonAgainstCondition(String condition, String jsonResponseString)
    {
        boolean result = false;
        try
        {
            JSONObject json = new JSONObject(new JSONTokener(jsonResponseString));
            Object aspectsObj = json.get("aspects");
            if (aspectsObj instanceof JSONArray)
            {
                JSONArray aspectsArray = (JSONArray) aspectsObj;
                for (int i = 0; i < aspectsArray.length(); i++)
                {
                    String nextAspect = aspectsArray.getString(i);

                    if (condition.equals(nextAspect))
                    {
                        result = true;
                        break;
                    }
                }
            }
        } catch (JSONException e)
        {
            if (getLogger().isWarnEnabled())
            {
                getLogger().warn("Failed to read JSON response from metadata service.", e);
            }
        }
        return result;
    }
}
