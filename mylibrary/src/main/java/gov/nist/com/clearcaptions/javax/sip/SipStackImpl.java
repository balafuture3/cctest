/*
 * Conditions Of Use 
 * 
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 * 
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 * 
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *  
 * .
 * 
 */
package gov.nist.com.clearcaptions.javax.sip;

import gov.nist.com.clearcaptions.core.LogWriter;
import gov.nist.com.clearcaptions.core.net.AddressResolver;
import gov.nist.com.clearcaptions.core.net.NetworkLayer;
import gov.nist.com.clearcaptions.core.net.SslNetworkLayer;
import gov.nist.com.clearcaptions.javax.sip.clientauthutils.AccountManager;
import gov.nist.com.clearcaptions.javax.sip.clientauthutils.AuthenticationHelper;
import gov.nist.com.clearcaptions.javax.sip.clientauthutils.AuthenticationHelperImpl;
import gov.nist.com.clearcaptions.javax.sip.parser.StringMsgParser;
import gov.nist.com.clearcaptions.javax.sip.stack.DefaultMessageLogFactory;
import gov.nist.com.clearcaptions.javax.sip.stack.DefaultRouter;
import gov.nist.com.clearcaptions.javax.sip.stack.MessageProcessor;
import gov.nist.com.clearcaptions.javax.sip.stack.SIPTransactionStack;
import gov.nist.com.clearcaptions.javax.sip.stack.ServerLog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;


import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

import com.clearcaptions.javax.sip.InvalidArgumentException;
import com.clearcaptions.javax.sip.ListeningPoint;
import com.clearcaptions.javax.sip.ObjectInUseException;
import com.clearcaptions.javax.sip.PeerUnavailableException;
import com.clearcaptions.javax.sip.ProviderDoesNotExistException;
import com.clearcaptions.javax.sip.SipException;
import com.clearcaptions.javax.sip.SipListener;
import com.clearcaptions.javax.sip.SipProvider;
import com.clearcaptions.javax.sip.SipStack;
import com.clearcaptions.javax.sip.TransportNotSupportedException;
import com.clearcaptions.javax.sip.address.Router;
import com.clearcaptions.javax.sip.header.HeaderFactory;
import com.clearcaptions.javax.sip.message.Request;

/**
 * Implementation of SipStack.
 * 
 * The JAIN-SIP stack is initialized by a set of properties (see the JAIN SIP documentation for an
 * explanation of these properties {@link SipStack} ). In addition to these, the
 * following are meaningful properties for the NIST SIP stack (specify these in the property array
 * when you create the JAIN-SIP statck):
 * <ul>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.TRACE_LEVEL = integer </b><br/> You can use the standard log4j
 * level names here (i.e. ERROR, INFO, WARNING, OFF, DEBUG) If this is set to INFO (or TRACE) or
 * above, then incoming valid messages are logged in SERVER_LOG. If you set this to 32 and specify
 * a DEBUG_LOG then vast amounts of trace information will be dumped in to the specified
 * DEBUG_LOG. The server log accumulates the signaling trace. <a href="{@docRoot}/tools/tracesviewer/tracesviewer.html">
 * This can be viewed using the trace viewer tool .</a> Please send us both the server log and
 * debug log when reporting non-obvious problems. You can also use the strings DEBUG or INFO for
 * level 32 and 16 respectively. If the value of this property is set to LOG4J, then the effective
 * log levels are determined from the log4j settings file (e.g. log4j.properties). The logger name
 * for the stack is specified using the gov.nist.com.clearcaptions.javax.sip.LOG4J_LOGGER_NAME property. By default
 * log4j logger name for the stack is the same as the stack name. For example, <code>
 *	properties.setProperty("gov.nist.com.clearcaptions.javax.sip.TRACE_LEVEL", "LOG4J");
 *	properties.setProperty("gov.nist.com.clearcaptions.javax.sip.LOG4J_LOGGER_NAME", "SIPStackLogger");
 * </code>
 * allows you to now control logging in the stack entirely using log4j facilities. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.SERVER_LOG = fileName </b><br/> Log valid incoming messages here.
 * If this is left null AND the TRACE_LEVEL is above INFO (or TRACE) then the messages are printed
 * to stdout. Otherwise messages are logged in a format that can later be viewed using the trace
 * viewer application which is located in the tools/tracesviewer directory. <font color=red> Mail
 * this to us with bug reports. </font> </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.LOG_MESSAGE_CONTENT = true|false </b><br/> Set true if you want to
 * capture content into the log. Default is false. A bad idea to log content if you are using SIP
 * to push a lot of bytes through TCP. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND = true|false </b><br/> Set true if you want to
 * to log a stack trace at INFO level for each message send. This is really handy 
 * for debugging.</li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.DEBUG_LOG = fileName </b><br/> Where the debug log goes. <font
 * color=red> Mail this to us with bug reports. </font> </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.MAX_MESSAGE_SIZE = integer</b> <br/> Maximum size of content that a
 * TCP connection can read. Must be at least 4K. Default is "infinity" -- ie. no limit. This is to
 * prevent DOS attacks launched by writing to a TCP connection until the server chokes. </li>
 * 
 * 
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.CACHE_SERVER_CONNECTIONS = [true|false] </b> <br/> Default value is
 * true. Setting this to false makes the Stack close the server socket after a Server Transaction
 * goes to the TERMINATED state. This allows a server to protectect against TCP based Denial of
 * Service attacks launched by clients (ie. initiate hundreds of client transactions). If true
 * (default action), the stack will keep the socket open so as to maximize performance at the
 * expense of Thread and memory resources - leaving itself open to DOS attacks. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.CACHE_CLIENT_CONNECTIONS = [true|false] </b> <br/> Default value is
 * true. Setting this to false makes the Stack close the server socket after a Client Transaction
 * goes to the TERMINATED state. This allows a client release any buffers threads and socket
 * connections associated with a client transaction after the transaction has terminated at the
 * expense of performance. </li>
 * 
 * <li> <b>gov.nist.com.clearcaptions.javax.sip.THREAD_POOL_SIZE = integer </b> <br/> Concurrency control for number
 * of simultaneous active threads. If unspecificed, the default is "infinity". This feature is
 * useful if you are trying to build a container.
 * <ul>
 * <li>
 * <li> If this is not specified, <b> and the listener is re-entrant</b>, each event delivered to
 * the listener is run in the context of a new thread. </li>
 * <li>If this is specified and the listener is re-entrant, then the stack will run the listener
 * using a thread from the thread pool. This allows you to manage the level of concurrency to a
 * fixed maximum. Threads are pre-allocated when the stack is instantiated.</li>
 * <li> If this is specified and the listener is not re-entrant, then the stack will use the
 * thread pool thread from this pool to parse and manage the state machine but will run the
 * listener in its own thread. </li>
 * </ul>
 * 
 * <li> <b>gov.nist.com.clearcaptions.javax.sip.REENTRANT_LISTENER = true|false </b> <br/> Default is false. Set to
 * true if the listener is re-entrant. If the listener is re-entrant then the stack manages a
 * thread pool and synchronously calls the listener from the same thread which read the message.
 * Multiple transactions may concurrently receive messages and this will result in multiple
 * threads being active in the listener at the same time. The listener has to be written with this
 * in mind. <b> If you want good performance on a multithreaded machine write your listener to be
 * re-entrant and set this property to be true </b></li>
 * 
 * <li> <b>gov.nist.com.clearcaptions.javax.sip.MAX_CONNECTIONS = integer </b> <br/> Max number of simultaneous TCP
 * connections handled by stack. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.MAX_SERVER_TRANSACTIONS = integer </b> <br/> Maximum size of server
 * transaction table. The low water mark is 80% of the high water mark. Requests are selectively
 * dropped in the lowater mark to highwater mark range. Requests are unconditionally accepted if
 * the table is smaller than the low water mark. The default highwater mark is 5000 </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.MAX_CLIENT_TRANSACTIONS = integer </b> <br/> Max number of active
 * client transactions before the caller blocks and waits for the number to drop below a
 * threshold. Default is unlimited, i.e. the caller never blocks and waits for a client
 * transaction to become available (i.e. it does its own resource management in the application).
 * </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER = true|false </b> <br/> If true
 * then the listener will see the ACK for non-2xx responses for server transactions. This is not
 * standard behavior per RFC 3261 (INVITE server transaction state machine) but this is a useful
 * flag for testing. The TCK uses this flag for example. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.MAX_LISTENER_RESPONSE_TIME = Integer </b> <br/>Max time (seconds)
 * before sending a response to a server transaction. If a response is not sent within this time
 * period, the transaction will be deleted by the stack. Default time is "infinity" - i.e. if the
 * listener never responds, the stack will hang on to a reference for the transaction and result
 * in a memory leak.
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.USE_TLS_ACCELERATOR = true|false </b> <br/>Default value is false.
 * Setting this to true permits the delegation of TLS encryption/decryption to an external, non
 * SIP-aware, TLS accelerator hardware. Such deployment requires the SIP stack to make its TLS
 * traffic goes over un-encrypted TCP connections to the TLS accelerator. So all TLS listening
 * points will be listening for plain TCP traffic, and outgoing messages sent with a TLS provider
 * will not be encrypted. Note that this does not affect the transport value in the Via header.
 * Another deployment strategy for TLS acceleration would be to use one or a cluster of outbound
 * proxies that transform the TCP or UDP SIP connection to TLS connections. This scenario does not
 * need the USE_TLS_ACCELERATOR switch, as the messages will be sent using a plain TCP or UDP
 * provider. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.DELIVER_TERMINATED_EVENT_FOR_ACK = [true|false]</b> <br/>Default is
 * <it>false</it>. ACK Server Transaction is a Pseuedo-transaction. If you want termination
 * notification on ACK transactions (so all server transactions can be handled uniformly in user
 * code during cleanup), then set this flag to <it>true</it>. </li>
 * 
 * <li> <b>gov.nist.com.clearcaptions.javax.sip.READ_TIMEOUT = integer </b> <br/> This is relevant for incoming TCP
 * connections to prevent starvation at the server. This defines the timeout in miliseconds
 * between successive reads after the first byte of a SIP message is read by the stack. All the
 * sip headers must be delivered in this interval and each successive buffer must be of the
 * content delivered in this interval. Default value is -1 (ie. the stack is wide open to
 * starvation attacks) and the client can be as slow as it wants to be. </li>
 * 
 * <li> <b>gov.nist.com.clearcaptions.javax.sip.NETWORK_LAYER = classpath </b> <br/> This is an EXPERIMENTAL
 * property (still under active devlopment). Defines a network layer that allows a client to have
 * control over socket allocations and monitoring of socket activity. A network layer should
 * implement gov.nist.com.clearcaptions.core.net.NetworkLayer. The default implementation simply acts as a wrapper
 * for the standard java.net socket layer. This functionality is still under active development
 * (may be extended to support security and other features). </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.ADDRESS_RESOLVER = classpath </b><br/> The fully qualified class
 * path for an implementation of the AddressResolver interface. The AddressResolver allows you to
 * support lookup schemes for addresses that are not directly resolvable to IP adresses using
 * getHostByName. Specifying your own address resolver allows you to customize address lookup. The
 * default address resolver is a pass-through address resolver (i.e. just returns the input string
 * without doing a resolution). See gov.nist.com.clearcaptions.javax.sip.DefaultAddressResolver. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.AUTO_GENERATE_TIMESTAMP= [true| false] </b><br/> (default is false)
 * Automatically generate a getTimeOfDay timestamp for a retransmitted request if the original
 * request contained a timestamp. This is useful for profiling. </li>
 * 
 * <li> <b>gov.nist.com.clearcaptions.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS = long </b> <br/> Defines how
 * often the application intends to audit the SIP Stack about the health of its internal threads
 * (the property specifies the time in miliseconds between successive audits). The audit allows
 * the application to detect catastrophic failures like an internal thread terminating because of
 * an exception or getting stuck in a deadlock condition. Events like these will make the stack
 * inoperable and therefore require immediate action from the application layer (e.g., alarms,
 * traps, reboot, failover, etc.) Thread audits are disabled by default. If this property is not
 * specified, audits will remain disabled. An example of how to use this property is in
 * src/examples/threadaudit. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.LOG_FACTORY = classpath </b> <br/> The fully qualified classpath for
 * an implementation of the MessageLogFactory. The stack calls the MessageLogFactory functions to
 * format the log for messages that are received or sent. This function allows you to log
 * auxiliary information related to the application or environmental conditions into the log
 * stream. The log factory must have a default constructor. </li>
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY = [true|false] </b> <br/>
 * Default is <it>false</it> If set to <it>true</it>, when you are creating a message from a
 * <it>String</it>, the MessageFactory will compute the content length from the message content
 * and ignore the provided content length parameter in the Message. Otherwise, it will use the
 * content length supplied and generate a parse exception if the content is truncated.
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED = [true|false] </b> <br/> Default
 * is <it>true</it>. This flag is added in support of load balancers or failover managers where
 * you may want to cancel ongoing transactions from a different stack than the original stack. If
 * set to <it>false</it> then the CANCEL client transaction is not checked for the existence of
 * the INVITE or the state of INVITE when you send the CANCEL request. Hence you can CANCEL an
 * INVITE from a different stack than the INVITE. You can also create a CANCEL client transaction
 * late and send it out after the INVITE server transaction has been Terminated. Clearly this will
 * result in protocol errors. Setting the flag to true ( default ) enables you to avoid common
 * protocol errors.
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.LOOSE_DIALOG_VALIDATION = [true|false] </b> <br/> Default
 * is <it>false</it>. This flag turns off some dialog validation features when the stack is used
 * in dialog-stateful mode. This means the validation is delegated to the application and the stack
 * will not attempt to block requests from reaching the application. In particular, the validation
 * of CSeq and the ACK retransmission recognition are delegated to the application. The stack will
 * no longer return an error when a CSeq number is out of order and it will no longer ignore incoming
 * ACK requests for the same dialog. Your application will be responsible for these cases.
 * <br/>This mode is needed for cases where multiple applications are using the same SipStack
 * but are unaware of each other, like Sip Servlets containers. In particular PROXY-B2BUA application
 * composion would cause an error when subsequent requests re-enter the container to reach the B2BUA
 * application. On the other hand if the ACK has to re-enter it would be rejected as a retransmission.
 * 
 * <li><b>gov.nist.com.clearcaptions.javax.sip.DELIVER_UNSOLICITED_NOTIFY = [true|false] </b> <br/> Default
 * is <it>false</it>. This flag is added to allow Sip Listeners to receive all NOTIFY requests
 * including those that are not part of a valid dialog.
 * 
 * <li><b>javax.net.ssl.keyStore = fileName </b> <br/> Default
 * is <it>NULL</it>.  If left undefined the keyStore and trustStore will be left to the main.java
 * runtime defaults.  If defined, any TLS sockets created (client and server) will use the 
 * key store provided in the fileName.  The trust store will default to the same store file.
 * A password must be provided to access the keyStore using the following property:
 * <br><code>
 *	properties.setProperty("javax.net.ssl.keyStorePassword", "&lt;password&gt;");
 * </code>
 * <br>The trust store can be changed, to a separate file with the following setting:
 * <br><code>
 *	properties.setProperty("javax.net.ssl.trustStore", "&lt;trustStoreFileName location&gt;");
 * </code>
 * <br>If the trust store property is provided the password on the trust store must be the same
 * as the key store. 
 * <br>
 * <br>
 * <b> Note that the stack supports the extensions that are defined in SipStackExt. These will be
 * supported in the next release of JAIN-SIP. You should only use the extensions that are defined
 * in this class. </b>
 * 
 * 
 * @version 1.2 $Revision: 1.87 $ $Date: 2009/06/23 11:02:17 $
 * 
 * @author M. Ranganathan <br/>
 * 
 * 
 * 
 * 
 */
public class SipStackImpl extends SIPTransactionStack implements SipStack, SipStackExt {

    private EventScanner eventScanner;

    private Hashtable<String, ListeningPointImpl> listeningPoints;

    private LinkedList<SipProviderImpl> sipProviders;

    // Flag to indicate that the listener is re-entrant and hence
    // Use this flag with caution.
    boolean reEntrantListener;

    SipListener sipListener;

    // If set to true then a transaction terminated event is
    // delivered for ACK transactions.
    boolean deliverTerminatedEventForAck = false;

    // If set to true then the application want to receive
    // unsolicited NOTIFYs, ie NOTIFYs that don't match any dialog
    boolean deliverUnsolicitedNotify = false;

    
    // RFC3261: TLS_RSA_WITH_AES_128_CBC_SHA MUST be supported
    // RFC3261: TLS_RSA_WITH_3DES_EDE_CBC_SHA SHOULD be supported for backwards compat
    private String[] cipherSuites = {
        "TLS_RSA_WITH_AES_128_CBC_SHA",     // AES difficult to get with c++/Windows
        // "TLS_RSA_WITH_3DES_EDE_CBC_SHA", // Unsupported by Sun impl,
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",    // For backwards comp., C++

        // JvB: patch from Sebastien Mazy, issue with mismatching ciphersuites
        "TLS_DH_anon_WITH_AES_128_CBC_SHA", 
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
    };
   

    /**
     * Creates a new instance of SipStackImpl.
     */

    protected SipStackImpl() {
        super();
        NistSipMessageFactoryImpl msgFactory = new NistSipMessageFactoryImpl(this);
        super.setMessageFactory(msgFactory);
        this.eventScanner = new EventScanner(this);
        this.listeningPoints = new Hashtable<String, ListeningPointImpl>();
        this.sipProviders = new LinkedList<SipProviderImpl>();

    }

    /**
     * ReInitialize the stack instance.
     */
    private void reInitialize() {
        super.reInit();
        this.eventScanner = new EventScanner(this);
        this.listeningPoints = new Hashtable<String, ListeningPointImpl>();
        this.sipProviders = new LinkedList<SipProviderImpl>();
        this.sipListener = null;

    }

    /**
     * Return true if automatic dialog support is enabled for this stack.
     * 
     * @return boolean, true if automatic dialog support is enabled for this stack
     */
    boolean isAutomaticDialogSupportEnabled() {
        return super.isAutomaticDialogSupportEnabled;
    }

    /**
     * Constructor for the stack.
     * 
     * @param configurationProperties -- stack configuration properties including NIST-specific
     *        extensions.
     * @throws PeerUnavailableException
     */
    public SipStackImpl(Properties configurationProperties) throws PeerUnavailableException {
        this();
        String address = configurationProperties.getProperty("com.clearcaptions.javax.sip.IP_ADDRESS");
        try {
            /** Retrieve the stack IP address */
            if (address != null) {
                // In version 1.2 of the spec the IP address is
                // associated with the listening point and
                // is not madatory.
                super.setHostAddress(address);

            }
        } catch (java.net.UnknownHostException ex) {
            throw new PeerUnavailableException("bad address " + address);
        }

        /** Retrieve the stack name */
        String name = configurationProperties.getProperty("com.clearcaptions.javax.sip.STACK_NAME");
        if (name == null)
            throw new PeerUnavailableException("stack name is missing");
        super.setStackName(name);
        // To log debug messages.
        this.logWriter = new LogWriter(configurationProperties);

        // Server log file.
        this.serverLog = new ServerLog(this, configurationProperties);

        // Default router -- use this for routing SIP URIs.
        // Our router does not do DNS lookups.
        this.outboundProxy = configurationProperties.getProperty("com.clearcaptions.javax.sip.OUTBOUND_PROXY");

        this.defaultRouter = new DefaultRouter(this, outboundProxy);

        /** Retrieve the router path */
        String routerPath = configurationProperties.getProperty("com.clearcaptions.javax.sip.ROUTER_PATH");
        if (routerPath == null)
            routerPath = "gov.nist.com.clearcaptions.javax.sip.stack.DefaultRouter";

        try {
            Class< ? > routerClass = Class.forName(routerPath);
            Class< ? >[] constructorArgs = new Class[2];
            constructorArgs[0] = SipStack.class;
            constructorArgs[1] = String.class;
            Constructor< ? > cons = routerClass.getConstructor(constructorArgs);
            Object[] args = new Object[2];
            args[0] = (SipStack) this;
            args[1] = outboundProxy;
            Router router = (Router) cons.newInstance(args);
            super.setRouter(router);
        } catch (InvocationTargetException ex1) {
            getLogWriter().logError("could not instantiate router -- invocation target problem",
                    (Exception) ex1.getCause());
            throw new PeerUnavailableException(
                    "Cound not instantiate router - check constructor", ex1);
        } catch (Exception ex) {
            getLogWriter().logError("could not instantiate router", (Exception) ex.getCause());
            throw new PeerUnavailableException("Could not instantiate router", ex);
        }

        // The flag that indicates that the default router is to be ignored.
        String useRouterForAll = configurationProperties
                .getProperty("com.clearcaptions.javax.sip.USE_ROUTER_FOR_ALL_URIS");
        this.useRouterForAll = true;
        if (useRouterForAll != null) {
            this.useRouterForAll = "true".equalsIgnoreCase(useRouterForAll);
        }

        /*
         * Retrieve the EXTENSION Methods. These are used for instantiation of Dialogs.
         */
        String extensionMethods = configurationProperties
                .getProperty("com.clearcaptions.javax.sip.EXTENSION_METHODS");

        if (extensionMethods != null) {
            StringTokenizer st = new StringTokenizer(extensionMethods);
            while (st.hasMoreTokens()) {
                String em = st.nextToken(":");
                if (em.equalsIgnoreCase(Request.BYE) || em.equalsIgnoreCase(Request.INVITE)
                        || em.equalsIgnoreCase(Request.SUBSCRIBE)
                        || em.equalsIgnoreCase(Request.NOTIFY)
                        || em.equalsIgnoreCase(Request.ACK)
                        || em.equalsIgnoreCase(Request.OPTIONS))
                    throw new PeerUnavailableException("Bad extension method " + em);
                else
                    this.addExtensionMethod(em);
            }
        }
        String keyStoreFile = configurationProperties.getProperty("javax.net.ssl.keyStore");
        String trustStoreFile = configurationProperties.getProperty("javax.net.ssl.trustStore");
        if (keyStoreFile != null) {
            if (trustStoreFile == null) {
                trustStoreFile = keyStoreFile;
            }
            String keyStorePassword = configurationProperties
                    .getProperty("javax.net.ssl.keyStorePassword");
            try {
                this.networkLayer = new SslNetworkLayer(trustStoreFile, keyStoreFile,
                        keyStorePassword.toCharArray(), configurationProperties
                                .getProperty("javax.net.ssl.keyStoreType"));
            } catch (Exception e1) {
                getLogWriter().logError("could not instantiate SSL networking", e1);
            }
        }

        // Set the auto dialog support flag.
        super.isAutomaticDialogSupportEnabled = configurationProperties.getProperty(
                "com.clearcaptions.javax.sip.AUTOMATIC_DIALOG_SUPPORT", "on").equalsIgnoreCase("on");
        
        super.looseDialogValidation = configurationProperties.getProperty(
                "gov.nist.com.clearcaptions.javax.sip.LOOSE_DIALOG_VALIDATION", "false").equalsIgnoreCase("true");

        if (configurationProperties.getProperty("gov.nist.com.clearcaptions.javax.sip.MAX_LISTENER_RESPONSE_TIME") != null) {
            super.maxListenerResponseTime = Integer.parseInt(configurationProperties
                    .getProperty("gov.nist.com.clearcaptions.javax.sip.MAX_LISTENER_RESPONSE_TIME"));
            if (super.maxListenerResponseTime <= 0)
                throw new PeerUnavailableException(
                        "Bad configuration parameter gov.nist.com.clearcaptions.javax.sip.MAX_LISTENER_RESPONSE_TIME : should be positive");
        } else {
            super.maxListenerResponseTime = -1;
        }

        this.useTlsAccelerator = false;
        String useTlsAcceleratorFlag = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.USE_TLS_ACCELERATOR");

        if (useTlsAcceleratorFlag != null
                && "true".equalsIgnoreCase(useTlsAcceleratorFlag.trim())) {
            this.useTlsAccelerator = true;
        }

        this.deliverTerminatedEventForAck = configurationProperties.getProperty(
                "gov.nist.com.clearcaptions.javax.sip.DELIVER_TERMINATED_EVENT_FOR_ACK", "false").equalsIgnoreCase(
                "true");

        this.deliverUnsolicitedNotify = configurationProperties.getProperty(
                "gov.nist.com.clearcaptions.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "false")
                .equalsIgnoreCase("true");

        String forkedSubscriptions = configurationProperties
                .getProperty("com.clearcaptions.javax.sip.FORKABLE_EVENTS");
        if (forkedSubscriptions != null) {
            StringTokenizer st = new StringTokenizer(forkedSubscriptions);
            while (st.hasMoreTokens()) {
                String nextEvent = st.nextToken();
                this.forkedEvents.add(nextEvent);
            }
        }

        // The following features are unique to the NIST implementation.

        /*
         * gets the NetworkLayer implementation, if any. Note that this is a NIST only feature.
         */

        final String NETWORK_LAYER_KEY = "gov.nist.com.clearcaptions.javax.sip.NETWORK_LAYER";

        if (configurationProperties.containsKey(NETWORK_LAYER_KEY)) {
            String path = configurationProperties.getProperty(NETWORK_LAYER_KEY);
            try {
                Class< ? > clazz = Class.forName(path);
                Constructor< ? > c = clazz.getConstructor(new Class[0]);
                networkLayer = (NetworkLayer) c.newInstance(new Object[0]);
            } catch (Exception e) {
                throw new PeerUnavailableException(
                        "can't find or instantiate NetworkLayer implementation: " + path);
            }
        }

        final String ADDRESS_RESOLVER_KEY = "gov.nist.com.clearcaptions.javax.sip.ADDRESS_RESOLVER";

        if (configurationProperties.containsKey(ADDRESS_RESOLVER_KEY)) {
            String path = configurationProperties.getProperty(ADDRESS_RESOLVER_KEY);
            try {
                Class< ? > clazz = Class.forName(path);
                Constructor< ? > c = clazz.getConstructor(new Class[0]);
                this.addressResolver = (AddressResolver) c.newInstance(new Object[0]);
            } catch (Exception e) {
                throw new PeerUnavailableException(
                        "can't find or instantiate AddressResolver implementation: " + path);
            }
        }

        String maxConnections = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.MAX_CONNECTIONS");
        if (maxConnections != null) {
            try {
                this.maxConnections = new Integer(maxConnections).intValue();
            } catch (NumberFormatException ex) {
                getLogWriter().logError("max connections - bad value " + ex.getMessage());
            }
        }

        String threadPoolSize = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.THREAD_POOL_SIZE");
        if (threadPoolSize != null) {
            try {
                this.threadPoolSize = new Integer(threadPoolSize).intValue();
            } catch (NumberFormatException ex) {
                this.logWriter.logError("thread pool size - bad value " + ex.getMessage());
            }
        }

        String serverTransactionTableSize = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.MAX_SERVER_TRANSACTIONS");
        if (serverTransactionTableSize != null) {
            try {
                this.serverTransactionTableHighwaterMark = new Integer(serverTransactionTableSize)
                        .intValue();
                this.serverTransactionTableLowaterMark = this.serverTransactionTableHighwaterMark * 80 / 100;
                // Lowater is 80% of highwater
            } catch (NumberFormatException ex) {
                this.logWriter.logError("transaction table size - bad value " + ex.getMessage());
            }
        }

        String clientTransactionTableSize = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.MAX_CLIENT_TRANSACTIONS");
        if (clientTransactionTableSize != null) {
            try {
                this.clientTransactionTableHiwaterMark = new Integer(clientTransactionTableSize)
                        .intValue();
                this.clientTransactionTableLowaterMark = this.clientTransactionTableLowaterMark * 80 / 100;
                // Lowater is 80% of highwater
            } catch (NumberFormatException ex) {
                this.logWriter.logError("transaction table size - bad value " + ex.getMessage());
            }
        } else {
            this.unlimitedClientTransactionTableSize = true;
        }

        super.cacheServerConnections = true;
        String flag = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.CACHE_SERVER_CONNECTIONS");

        if (flag != null && "false".equalsIgnoreCase(flag.trim())) {
            super.cacheServerConnections = false;
        }

        super.cacheClientConnections = true;
        String cacheflag = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.CACHE_CLIENT_CONNECTIONS");

        if (cacheflag != null && "false".equalsIgnoreCase(cacheflag.trim())) {
            super.cacheClientConnections = false;
        }

        String readTimeout = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.READ_TIMEOUT");
        if (readTimeout != null) {
            try {

                int rt = Integer.parseInt(readTimeout);
                if (rt >= 100) {
                    super.readTimeout = rt;
                } else {
                    System.err.println("Value too low " + readTimeout);
                }
            } catch (NumberFormatException nfe) {
                // Ignore.
                getLogWriter().logError("Bad read timeout " + readTimeout);
            }
        }

        // Get the address of the stun server.

        String stunAddr = configurationProperties.getProperty("gov.nist.com.clearcaptions.javax.sip.STUN_SERVER");

        if (stunAddr != null)
            this.logWriter.logWarning("Ignoring obsolete property "
                    + "gov.nist.com.clearcaptions.javax.sip.STUN_SERVER");

        String maxMsgSize = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.MAX_MESSAGE_SIZE");

        try {
            if (maxMsgSize != null) {
                super.maxMessageSize = new Integer(maxMsgSize).intValue();
                if (super.maxMessageSize < 4096)
                    super.maxMessageSize = 4096;
            } else {
                // Allow for "infinite" size of message
                super.maxMessageSize = 0;
            }
        } catch (NumberFormatException ex) {
            getLogWriter().logError("maxMessageSize - bad value " + ex.getMessage());
        }

        String rel = configurationProperties.getProperty("gov.nist.com.clearcaptions.javax.sip.REENTRANT_LISTENER");
        this.reEntrantListener = (rel != null && "true".equalsIgnoreCase(rel));

        // Check if a thread audit interval is specified
        String interval = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS");
        if (interval != null) {
            try {
                // Make the monitored threads ping the auditor twice as fast as
                // the audits
                getThreadAuditor().setPingIntervalInMillisecs(
                        Long.valueOf(interval).longValue() / 2);
            } catch (NumberFormatException ex) {
                getLogWriter().logError("THREAD_AUDIT_INTERVAL_IN_MILLISECS - bad value [" + interval
                        + "] " + ex.getMessage());
            }
        }

        // JvB: added property for testing
        this.setNon2XXAckPassedToListener(Boolean.valueOf(
                configurationProperties.getProperty(
                        "gov.nist.com.clearcaptions.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "false"))
                .booleanValue());

        this.generateTimeStampHeader = Boolean.valueOf(
                configurationProperties.getProperty("gov.nist.com.clearcaptions.javax.sip.AUTO_GENERATE_TIMESTAMP",
                        "false")).booleanValue();

        String messageLogFactoryClasspath = configurationProperties
                .getProperty("gov.nist.com.clearcaptions.javax.sip.LOG_FACTORY");
        if (messageLogFactoryClasspath != null) {
            try {
                Class< ? > clazz = Class.forName(messageLogFactoryClasspath);
                Constructor< ? > c = clazz.getConstructor(new Class[0]);
                this.logRecordFactory = (LogRecordFactory) c.newInstance(new Object[0]);
            } catch (Exception ex) {
                getLogWriter().logError("Bad configuration value for LOG_FACTORY -- using default logger");
                this.logRecordFactory = new DefaultMessageLogFactory();
            }

        } else {
            this.logRecordFactory = new DefaultMessageLogFactory();
        }

        boolean computeContentLength = configurationProperties.getProperty(
                "gov.nist.com.clearcaptions.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "false")
                .equalsIgnoreCase("true");
        StringMsgParser.setComputeContentLengthFromMessage(computeContentLength);

        super.rfc2543Supported = configurationProperties.getProperty(
                "gov.nist.com.clearcaptions.javax.sip.RFC_2543_SUPPORT_ENABLED", "true").equalsIgnoreCase("true");

        super.cancelClientTransactionChecked = configurationProperties.getProperty(
                "gov.nist.com.clearcaptions.javax.sip.CANCEL_CLIENT_TRANSACTION_CHECKED", "true").equalsIgnoreCase(
                "true");
        super.logStackTraceOnMessageSend = configurationProperties.getProperty(
                "gov.nist.com.clearcaptions.javax.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND", "false").equalsIgnoreCase("true");
        logWriter.logDebug("created Sip stack. Properties = " + configurationProperties);
        InputStream in = getClass().getResourceAsStream("/TIMESTAMP");
        if (in != null) {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in));

            try {
                String buildTimeStamp = streamReader.readLine();
                if (in != null) {
                    in.close();
                }
                getLogWriter().setBuildTimeStamp(buildTimeStamp);
            } catch (IOException ex) {
                getLogWriter().logError("Could not open build timestamp.");
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#createListeningPoint(java.lang.String, int, java.lang.String)
     */
    public synchronized ListeningPoint createListeningPoint(String address, int port,
            String transport) throws TransportNotSupportedException, InvalidArgumentException {
        getLogWriter().logDebug(
                "createListeningPoint : address = " + address + " port = " + port
                        + " transport = " + transport);

        if (address == null)
            throw new NullPointerException("Address for listening point is null!");
        if (transport == null)
            throw new NullPointerException("null transport");
        if (port <= 0)
            throw new InvalidArgumentException("bad port");

        if (!transport.equalsIgnoreCase("UDP") && !transport.equalsIgnoreCase("TLS") 
                && !transport.equalsIgnoreCase("TCP"))
            throw new TransportNotSupportedException("bad transport " + transport);

        /** Reusing an old stack instance */
        if (!this.isAlive()) {
            this.toExit = false;
            this.reInitialize();
        }

        String key = ListeningPointImpl.makeKey(address, port, transport);

        ListeningPointImpl lip = (ListeningPointImpl) listeningPoints.get(key);
        if (lip != null) {
            return lip;
        } else {
            try {
                InetAddress inetAddr = InetAddress.getByName(address);
                MessageProcessor messageProcessor = this.createMessageProcessor(inetAddr, port,
                        transport);
                if (this.isLoggingEnabled()) {
                    this.getLogWriter().logDebug(
                            "Created Message Processor: " + address + " port = " + port
                                    + " transport = " + transport);
                }
                lip = new ListeningPointImpl(this, port, transport);
                lip.messageProcessor = messageProcessor;
                messageProcessor.setListeningPoint(lip);
                this.listeningPoints.put(key, lip);
                // start processing messages.
                messageProcessor.start();
                return (ListeningPoint) lip;
            } catch (IOException ex) {
                getLogWriter().logError(
                        "Invalid argument address = " + address + " port = " + port
                                + " transport = " + transport);
                throw new InvalidArgumentException(ex.getMessage(), ex);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#createSipProvider(com.clearcaptions.javax.sip.ListeningPoint)
     */
    public SipProvider createSipProvider(ListeningPoint listeningPoint)
            throws ObjectInUseException {
        if (listeningPoint == null)
            throw new NullPointerException("null listeningPoint");
        if (this.getLogWriter().isLoggingEnabled())
            this.getLogWriter().logDebug("createSipProvider: " + listeningPoint);
        ListeningPointImpl listeningPointImpl = (ListeningPointImpl) listeningPoint;
        if (listeningPointImpl.sipProvider != null)
            throw new ObjectInUseException("Provider already attached!");

        SipProviderImpl provider = new SipProviderImpl(this);

        provider.setListeningPoint(listeningPointImpl);
        listeningPointImpl.sipProvider = provider;
        this.sipProviders.add(provider);
        return provider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#deleteListeningPoint(com.clearcaptions.javax.sip.ListeningPoint)
     */
    public void deleteListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        if (listeningPoint == null)
            throw new NullPointerException("null listeningPoint arg");
        ListeningPointImpl lip = (ListeningPointImpl) listeningPoint;
        // Stop the message processing thread in the listening point.
        super.removeMessageProcessor(lip.messageProcessor);
        String key = lip.getKey();
        this.listeningPoints.remove(key);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#deleteSipProvider(com.clearcaptions.javax.sip.SipProvider)
     */
    public void deleteSipProvider(SipProvider sipProvider) throws ObjectInUseException {

        if (sipProvider == null)
            throw new NullPointerException("null provider arg");
        SipProviderImpl sipProviderImpl = (SipProviderImpl) sipProvider;

        // JvB: API doc is not clear, but in_use ==
        // sipProviderImpl.sipListener!=null
        // so we should throw if app did not call removeSipListener
        // sipProviderImpl.sipListener = null;
        sipProviderImpl.removeSipListener(sipProviderImpl.sipListener);
        if (sipProviderImpl.sipListener != null) {
            throw new ObjectInUseException("SipProvider still has an associated SipListener!");
        }

        sipProviderImpl.removeListeningPoints();

        // Bug reported by Rafael Barriuso
        sipProviderImpl.stop();
        sipProviders.remove(sipProvider);
        if (sipProviders.isEmpty()) {
            this.stopStack();
        }
    }

    /**
     * Get the IP Address of the stack.
     * 
     * @see SipStack#getIPAddress()
     * @deprecated
     */
    public String getIPAddress() {
        return super.getHostAddress();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#getListeningPoints()
     */
    public java.util.Iterator getListeningPoints() {
        return this.listeningPoints.values().iterator();
    }

    /**
     * Return true if retransmission filter is active.
     * 
     * @see SipStack#isRetransmissionFilterActive()
     * @deprecated
     */
    public boolean isRetransmissionFilterActive() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#getSipProviders()
     */
    public java.util.Iterator<SipProviderImpl> getSipProviders() {
        return this.sipProviders.iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#getStackName()
     */
    public String getStackName() {
        return this.stackName;
    }

    /**
     * Finalization -- stop the stack on finalization. Exit the transaction scanner and release
     * all resources.
     * 
     * @see Object#finalize()
     */
    protected void finalize() {
        this.stopStack();
    }

    /**
     * This uses the default stack address to create a listening point.
     * 
     * @see SipStack#createListeningPoint(String, int, String)
     * @deprecated
     */
    public ListeningPoint createListeningPoint(int port, String transport)
            throws TransportNotSupportedException, InvalidArgumentException {
        if (super.stackAddress == null)
            throw new NullPointerException("Stack does not have a default IP Address!");
        return this.createListeningPoint(super.stackAddress, port, transport);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#stop()
     */
    public void stop() {
        if (getLogWriter().isLoggingEnabled()) {
            getLogWriter().logDebug("stopStack -- stoppping the stack");
        }
        this.stopStack();
        this.sipProviders = new LinkedList<SipProviderImpl>();
        this.listeningPoints = new Hashtable<String, ListeningPointImpl>();
        /*
         * Check for presence of an event scanner ( may happen if stack is stopped before listener
         * is attached ).
         */
        if (this.eventScanner != null)
            this.eventScanner.forceStop();
        this.eventScanner = null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.clearcaptions.javax.sip.SipStack#start()
     */
    public void start() throws ProviderDoesNotExistException, SipException {
        // Start a new event scanner if one does not exist.
        if (this.eventScanner == null) {
            this.eventScanner = new EventScanner(this);
        }

    }

    /**
     * Get the listener for the stack. A stack can have only one listener. To get an event from a
     * provider, the listener has to be registered with the provider. The SipListener is
     * application code.
     * 
     * @return -- the stack SipListener
     * 
     */
    protected SipListener getSipListener() {
        return this.sipListener;
    }

    /**
     * Get the message log factory registered with the stack.
     * 
     * @return -- the messageLogFactory of the stack.
     */
    public LogRecordFactory getLogRecordFactory() {
        return super.logRecordFactory;
    }

    /**
     * Set the log appender ( this is useful if you want to specify a particular log format or log
     * to something other than a file for example).
     * 
     * @param Appender - the log4j appender to add.
     * 
     */
    public void addLogAppender(Appender appender) {
        this.getLogWriter().addAppender(appender);
    }

    /**
     * Get the log4j logger ( for log stream integration ).
     * 
     * @return
     */
    public Logger getLogger() {
        return this.getLogWriter().getLogger();
    }

    public EventScanner getEventScanner() {
        return eventScanner;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.nist.com.clearcaptions.javax.sip.SipStackExt#getAuthenticationHelper(gov.nist.com.clearcaptions.javax.sip.clientauthutils.AccountManager,
     *      com.clearcaptions.javax.sip.header.HeaderFactory)
     */
    public AuthenticationHelper getAuthenticationHelper(AccountManager accountManager,
            HeaderFactory headerFactory) {
        return new AuthenticationHelperImpl(this, accountManager, headerFactory);
    }
    
    /**
     * Set the list of cipher suites supported by the stack.  A stack can have only one set of suites.
     * These are not validated against the supported cipher suites of the main.java runtime, so specifying
     * a cipher here does not guarantee that it will work.<br> 
     * The stack has a default cipher suite of:
     * <ul>
     * <li> TLS_RSA_WITH_AES_128_CBC_SHA </li>
     * <li> SSL_RSA_WITH_3DES_EDE_CBC_SHA </li>
     * <li> TLS_DH_anon_WITH_AES_128_CBC_SHA </li>
     * <li> SSL_DH_anon_WITH_3DES_EDE_CBC_SHA </li>
     * </ul>
     * 
     * <b>NOTE: This function must be called before adding a TLS listener</b>
     * 
     * @param String[] The new set of ciphers to support.
     * @return
     * 
     */
    public void setEnabledCipherSuites(String []newCipherSuites) {
    	cipherSuites = newCipherSuites;
    }

    /**
     * Return the currently enabled cipher suites of the Stack.
     * 
     * @return The currently enabled cipher suites.
     */
    public String []getEnabledCipherSuites() {
    	return cipherSuites;
    }
}
