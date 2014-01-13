package com.blazebit.security.auth.login;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

public class ADAuthenticator {

    private String domain;
    private String ldapHost;
    private String searchBase;

    public ADAuthenticator() {
        this.domain = "<your domain>";
        this.ldapHost = "ldap://<your AD controller>";
        this.searchBase = "your AD root e.g. dc=abbl,dc=org";
    }

    public ADAuthenticator(String domain, String host, String dn) {
        this.domain = domain;
        this.ldapHost = host;
        this.searchBase = dn;
    }

    public Map authenticate(String user, String pass) {
        String returnedAtts[] = { "sn", "givenName", "mail" };
        String searchFilter = "(&(objectClass=user)(sAMAccountName=" + user + "))";

        // Create the search controls
        SearchControls searchCtls = new SearchControls();
        searchCtls.setReturningAttributes(returnedAtts);

        // Specify the search scope
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapHost);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, user + "@" + domain);
        env.put(Context.SECURITY_CREDENTIALS, pass);

        LdapContext ctxGC = null;

        try {
            ctxGC = new InitialLdapContext(env, null);
            // Search objects in GC using filters
            NamingEnumeration answer = ctxGC.search(searchBase, searchFilter, searchCtls);
            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.next();
                Attributes attrs = sr.getAttributes();
                Map amap = null;
                if (attrs != null) {
                    amap = new HashMap();
                    NamingEnumeration ne = attrs.getAll();
                    while (ne.hasMore()) {
                        Attribute attr = (Attribute) ne.next();
                        amap.put(attr.getID(), attr.get());
                    }
                    ne.close();
                }
                return amap;
            }
        } catch (NamingException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
