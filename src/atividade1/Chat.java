package atividade1;

import java.util.Scanner;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Chat {

	private static final String EXCHANGE_NAME = "chat";
	

	public static void main(String[] args) throws Exception {
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Nome de usuário: ");
		String username = scan.nextLine();
		
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("127.0.0.1");
		factory.setUsername("guest");
		factory.setPassword("guest");
		factory.setVirtualHost("/");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		
		channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
		channel.queueDeclare(username, false, false, false, null);
		
		receptor(channel, username);
		emissor(channel, scan, username);
		
		scan.close();
		channel.close();
		connection.close();
	}
	
	private static void emissor(Channel channel, Scanner scan, String username) throws Exception{
		while(true) {
			String mensagem = scan.nextLine();
			// Condição de parada
			if(mensagem.equals("sair")) {
				mensagem = " [!] " + username+" quitou do chat!";
				channel.basicPublish(EXCHANGE_NAME, "", null, mensagem.getBytes("UTF-8"));
				break;
			}else{
				// mensagem privada
				if(mensagem.charAt(0) == '@') {
					int i = 1;
					while(mensagem.charAt(i) != ' ') {
						i++;
					}
					String name_user = mensagem.substring(1, i);
					mensagem = "[Privado] " + username + ": " + mensagem.substring(i+1, mensagem.length());
					channel.basicPublish("", name_user, null, mensagem.getBytes("UTF-8"));
				// mensagem geral
				}else {
					mensagem = "[Geral] " + username + ": " + mensagem;
					channel.basicPublish(EXCHANGE_NAME, "", null, mensagem.getBytes("UTF-8"));
				}
				//System.out.println(" [x] " + username + ": '" + mensagem + "'");
			}
		}
	}
	
	private static void receptor(Channel channel, String username) throws Exception {
		String nomeFila = channel.queueDeclare().getQueue();
		channel.queueBind(nomeFila, EXCHANGE_NAME, "");

		String mensagem;
		
		mensagem = " [!] " + username +" spawnou no Chat!";
		channel.basicPublish(EXCHANGE_NAME, "", null, mensagem.getBytes("UTF-8"));
		
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), "UTF-8");
			System.out.println(message);
		};
		
		// fanout
		channel.basicConsume(nomeFila, true, deliverCallback, consumerTag -> {});
		// direct
		channel.basicConsume(username, true, deliverCallback, consumerTag -> {});
	}

}
