package com.ably.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.Channel.MessageListener;
import io.ably.lib.realtime.ChannelState;
import io.ably.lib.realtime.ChannelStateListener;
import io.ably.lib.realtime.CompletionListener;
import io.ably.lib.realtime.Presence.PresenceListener;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.ErrorInfo;
import io.ably.lib.types.Message;
import io.ably.lib.types.PaginatedResult;
import io.ably.lib.types.PresenceMessage;

public class App {

	static AblyRealtime ablyRealtime;
	static Channel channel;

	public static void main(String[] args) throws AblyException, IOException {
		if (args.length == 0) {
			System.out.println("You must send the Ably token as parameter");
			System.exit(-1);
		}
		
		ClientOptions opt = new ClientOptions(args[0]);
		opt.clientId = "client-" + System.currentTimeMillis();

		ablyRealtime = new AblyRealtime(opt);
		channel = ablyRealtime.channels.get("push:sample_channel");

		enter();
		listen();

	}

	private static void enter() throws AblyException {
		channel.presence.enter("online", new CompletionListener() {

			public void onSuccess() {
				// Successfully entered to the channel
			}

			public void onError(ErrorInfo reason) {
				// Failed to enter channel
			}

		});

	}

	private static void listen() throws IOException, AblyException {

		// on channel message receive
		channel.subscribe(new MessageListener() {
			public void onMessage(Message message) {
				System.out.println(message.clientId + ":  " + message.data);
			}
		});

		// //on channel state change
		channel.on(new ChannelStateListener() {
			public void onChannelStateChanged(ChannelState state, ErrorInfo reason) {
				System.out.println("Channel state changed to " + state.name());
				if (reason != null)
					System.out.println(reason.toString());
			}
		});

		// subscribe to channel
		channel.presence.subscribe(new PresenceListener() {
			public void onPresenceMessage(PresenceMessage member) {
				switch (member.action) {
				case enter: {
					System.out.println(member.clientId + " has entered");
					break;
				}
				case update: {
					System.out.println(member.clientId + " has changed status to " + member.data);
					break;
				}
				case leave: {
					System.out.println(member.clientId + " has left");
					break;
				}
				case absent: {
					System.out.println("absented: " + member.data);
					break;
				}

				case present: {
					System.out.println(member.clientId + " is present with status " + member.data);
					break;
				}
				}
			}
		});

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";

		while (line.equalsIgnoreCase("quit") == false) {
			line = in.readLine();

			handleInput(line);
		}

		channel.unsubscribe();
		in.close();
		System.out.println("thank you :)");
		System.exit(0);
	}

	private static void handleInput(String line) throws AblyException {
		if (line.trim().equals("help")) {
			System.out.println("Available commands:");
			System.out.println("\tlist - list all active users in the channel");
			System.out.println("\tsend [message] - send a message to channel");
			System.out.println("\tstatus [new status] - to change your status on channel");
			System.out.println("\tkick [clientId] - kick the user from the channel");
			return;
		}

		if (line.trim().equals("list")) {
			System.out.println("Active users:");

			PresenceMessage[] presenceMessages = channel.presence.get();
			for (PresenceMessage presence : presenceMessages) {
				System.out.println("\t" + presence.clientId + "[" + presence.data + "] / " + presence.connectionId
						+ " / " + presence.id);
			}

			return;
		}

		if (line.trim().startsWith("send ")) {
			String message = line.substring(5, line.length());

			Message[] messages = new Message[] { new Message("message", message) };
			channel.publish(messages);
			return;
		}

		if (line.trim().startsWith("status ")) {
			String message = line.substring(7, line.length());

			channel.presence.update(message, new CompletionListener() {
				public void onSuccess() {

				}

				public void onError(ErrorInfo arg0) {

				}
			});
			return;
		}

		if (line.trim().equals("history")) {
			PaginatedResult<Message> history = channel.history(null);

			if (history.hasNext()) {
				Message[] messages = new Message[] { new Message("message", "showing last 10 messages") };
				channel.publish(messages);
				for (Message message : history.next().items()) {
					channel.publish(message);
				}
			}

			return;
		}

		if (line.trim().startsWith("kick ")) {
			String clientId = line.substring(5, line.length());

			System.out.println("kicking user " + clientId);

			channel.presence.leaveClient(clientId, "was kicked", new CompletionListener() {
				public void onSuccess() {
					System.out.println("kick success");
				}

				public void onError(ErrorInfo error) {
					System.out.println(error.message);
				}
			});
			return;
		}

	}
}
