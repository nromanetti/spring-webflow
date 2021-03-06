package org.springframework.faces.webflow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.springframework.binding.message.DefaultMessageContext;
import org.springframework.binding.message.Message;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.webflow.execution.RequestContext;

public class FlowFacesContextTests extends TestCase {

	JSFMockHelper jsf = new JSFMockHelper();

	FacesContext facesContext;

	RequestContext requestContext;

	MessageContext messageContext;

	MessageContext prepopulatedMessageContext;

	@SuppressWarnings("cast")
	protected void setUp() throws Exception {
		jsf.setUp();
		requestContext = (RequestContext) EasyMock.createMock(RequestContext.class);
		facesContext = new Jsf2FlowFacesContext(requestContext, jsf.facesContext());

		setupMessageContext();
	}

	protected void tearDown() throws Exception {
		jsf.tearDown();
	}

	public final void testCurrentInstance() {
		assertSame(FacesContext.getCurrentInstance(), facesContext);
	}

	public final void testAddMessage() {
		messageContext = new DefaultMessageContext();
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		facesContext.addMessage("foo", new FacesMessage(FacesMessage.SEVERITY_INFO, "foo", "bar"));

		assertEquals("Message count is incorrect", 2, messageContext.getAllMessages().length);
		Message summaryMessage = messageContext.getMessagesBySource("foo_summary")[0];
		assertEquals("foo", summaryMessage.getText());
		Message detailMessage = messageContext.getMessagesBySource("foo_detail")[0];
		assertEquals("bar", detailMessage.getText());
	}

	public final void testGetGlobalMessagesOnly() {
		messageContext = new DefaultMessageContext();
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		facesContext.addMessage("foo", new FacesMessage(FacesMessage.SEVERITY_INFO, "foo", "bar"));
		facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "FOO", "BAR"));

		int iterationCount = 0;
		Iterator<FacesMessage> i = facesContext.getMessages(null);
		while (i.hasNext()) {
			FacesMessage message = i.next();
			assertNotNull(message);
			iterationCount++;
		}
		assertEquals(1, iterationCount);
	}

	public final void testGetAllMessages() {
		messageContext = new DefaultMessageContext();
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		facesContext.addMessage("foo", new FacesMessage(FacesMessage.SEVERITY_INFO, "foo", "bar"));
		facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "FOO", "BAR"));

		int iterationCount = 0;
		Iterator<FacesMessage> i = facesContext.getMessages();
		while (i.hasNext()) {
			FacesMessage message = i.next();
			assertNotNull(message);
			iterationCount++;
		}
		assertEquals(2, iterationCount);
	}

	public final void testAddMessages_MultipleNullIds() {
		messageContext = new DefaultMessageContext();
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "foo", "bar"));
		facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "zoo", "zar"));

		assertEquals("Message count is incorrect", 4, messageContext.getAllMessages().length);
		Message summaryMessage1 = messageContext.getMessagesBySource("null_summary")[0];
		assertEquals("foo", summaryMessage1.getText());
		Message detailMessage1 = messageContext.getMessagesBySource("null_detail")[0];
		assertEquals("bar", detailMessage1.getText());
		Message summaryMessage2 = messageContext.getMessagesBySource("null_summary")[1];
		assertEquals("zoo", summaryMessage2.getText());
		Message detailMessage2 = messageContext.getMessagesBySource("null_detail")[1];
		assertEquals("zar", detailMessage2.getText());
	}

	public final void testGetMessages() {
		messageContext = prepopulatedMessageContext;
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		int iterationCount = 0;
		Iterator<FacesMessage> i = facesContext.getMessages();
		while (i.hasNext()) {
			assertNotNull(i.next());
			iterationCount++;
		}
		assertEquals("There should be 6 messages to iterate", 6, iterationCount);
	}

	public final void testMutableGetMessages() {
		messageContext = prepopulatedMessageContext;
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		facesContext.addMessage("TESTID", new FacesMessage("summary1"));
		FacesMessage soruceMessage = facesContext.getMessages("TESTID").next();
		soruceMessage.setSummary("summary2");

		// check that message sticks around even when the facesContext has been torn down and re-created during the
		// processing of the current request
		FacesContext newFacesContext = new FlowFacesContext(requestContext, jsf.facesContext());
		assertSame(FacesContext.getCurrentInstance(), newFacesContext);

		FacesMessage gotMessage = newFacesContext.getMessages("TESTID").next();
		assertEquals("summary2", gotMessage.getSummary());
	}

	public final void testGetMessagesByClientId_ForComponent() {
		messageContext = prepopulatedMessageContext;
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		int iterationCount = 0;
		Iterator<FacesMessage> i = facesContext.getMessages("componentId");
		while (i.hasNext()) {
			FacesMessage message = i.next();
			assertNotNull(message);
			assertEquals("componentId_summary" + (iterationCount + 1), message.getSummary());
			assertEquals("componentId_detail" + (iterationCount + 1), message.getDetail());
			iterationCount++;
		}
		assertEquals(2, iterationCount);
	}

	public final void testGetMessagesByClientId_ForUserMessage() {
		messageContext = prepopulatedMessageContext;
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		int iterationCount = 0;
		Iterator<FacesMessage> i = facesContext.getMessages("userMessage");
		while (i.hasNext()) {
			FacesMessage message = i.next();
			assertNotNull(message);
			assertEquals("userMessage", message.getSummary());
			assertEquals("userMessage", message.getDetail());
			iterationCount++;
		}
		assertEquals(1, iterationCount);
	}

	public final void testgetMessagesByClientId_InvalidId() {
		messageContext = prepopulatedMessageContext;
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		Iterator<FacesMessage> i = facesContext.getMessages("unknown");
		assertFalse(i.hasNext());
	}

	public final void testGetClientIdsWithMessages() {
		messageContext = prepopulatedMessageContext;
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		List<String> expectedOrderedIds = new ArrayList<String>();
		expectedOrderedIds.add(null);
		expectedOrderedIds.add("componentId");
		expectedOrderedIds.add("userMessage");

		int iterationCount = 0;
		Iterator<String> i = facesContext.getClientIdsWithMessages();
		while (i.hasNext()) {
			String clientId = i.next();
			assertEquals("Client id not expected", expectedOrderedIds.get(iterationCount), clientId);
			iterationCount++;
		}
		assertEquals(3, iterationCount);
	}

	public final void testMessagesAreSerializable() throws Exception {
		DefaultMessageContext messageContext = new DefaultMessageContext();
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		facesContext.addMessage("TESTID", new FacesMessage("summary1"));
		FacesMessage sourceMessage = facesContext.getMessages("TESTID").next();
		sourceMessage.setSummary("summary2");
		sourceMessage.setSeverity(FacesMessage.SEVERITY_FATAL);

		Serializable mementoWrite = messageContext.createMessagesMemento();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(mementoWrite);
		oos.flush();
		byte[] byteArray = bos.toByteArray();
		oos.close();

		ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
		ObjectInputStream ois = new ObjectInputStream(bis);
		Serializable mementoRead = (Serializable) ois.readObject();
		ois.close();

		messageContext.restoreMessages(mementoRead);
		EasyMock.reset(new Object[] { requestContext });
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		FacesContext newFacesContext = new FlowFacesContext(requestContext, jsf.facesContext());
		assertSame(FacesContext.getCurrentInstance(), newFacesContext);
		FacesMessage gotMessage = newFacesContext.getMessages("TESTID").next();
		assertEquals("summary2", gotMessage.getSummary());
		assertEquals(FacesMessage.SEVERITY_FATAL, gotMessage.getSeverity());
	}

	public final void testGetMaximumSeverity() {
		messageContext = prepopulatedMessageContext;
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		assertEquals(FacesMessage.SEVERITY_FATAL, facesContext.getMaximumSeverity());
	}

	public final void testGetELContext() {

		assertNotNull(facesContext.getELContext());
		assertSame(facesContext, facesContext.getELContext().getContext(FacesContext.class));
	}

	public final void testValidationFailed() {
		messageContext = new DefaultMessageContext();
		EasyMock.expect(requestContext.getMessageContext()).andStubReturn(messageContext);
		EasyMock.replay(new Object[] { requestContext });

		facesContext.addMessage("foo", new FacesMessage(FacesMessage.SEVERITY_ERROR, "foo", "bar"));

		assertEquals(true, facesContext.isValidationFailed());
	}

	private void setupMessageContext() {
		prepopulatedMessageContext = new DefaultMessageContext();
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("null_summary").defaultText("foo").info()
				.build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("null_detail").defaultText("foo").info()
				.build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("componentId_summary")
				.defaultText("componentId_summary1").warning().build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("componentId_detail")
				.defaultText("componentId_detail1").warning().build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("componentId_summary")
				.defaultText("componentId_summary2").warning().build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("componentId_detail")
				.defaultText("componentId_detail2").warning().build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("userMessage").defaultText("userMessage")
				.info().build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("null_summary").defaultText("baz").error()
				.build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().source("null_detail").defaultText("baz").error()
				.build());
		prepopulatedMessageContext.addMessage(new MessageBuilder().defaultText("Subzero Wins - Fatality").fatal()
				.build());
	}
}
