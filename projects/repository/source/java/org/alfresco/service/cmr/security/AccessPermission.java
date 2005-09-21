/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.service.cmr.security;


/**
 * The interface used to support reporting back if permissions are allowed or
 * denied.
 * 
 * @author Andy Hind
 */
public interface AccessPermission
{   
    /**
     * The permission.
     * 
     * @return
     */
    public String getPermission();
    
    /**
     * Get the Access enumeration value
     * 
     * @return
     */
    public AccessStatus getAccessStatus();
    
    
    /**
     * Get the authority to which this permission applies.
     * 
     * @return
     */
    public String getAuthority();
    
 
    /**
     * Get the type of authority to which this permission applies.
     * 
     * @return
     */
    public AuthorityType getAuthorityType();
}
