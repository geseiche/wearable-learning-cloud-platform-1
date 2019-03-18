package wlcp.gameserver.spring.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import wlcp.gameserver.spring.repository.GameInstanceRepository;
import wlcp.gameserver.spring.repository.GameRepository;
import wlcp.gameserver.spring.repository.UsernameRepository;
import wlcp.gameserver.spring.service.GameInstanceService;
import wlcp.model.master.GameInstance;
import wlcp.model.master.Username;
import wlcp.shared.message.ConnectRequestMessage;
import wlcp.shared.message.DisconnectResponseMessage;
import wlcp.shared.message.IMessage;
import wlcp.shared.message.PlayerAvaliableMessage;

@RestController
@RequestMapping("/controllers")
@CrossOrigin
public class GameInstanceController {
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
	private GameRepository gameRepository;
	
	@Autowired
	private GameInstanceRepository gameInstanceRepository;
	
	@Autowired
	private UsernameRepository usernameRepository;
	
	@Autowired
	SimpMessagingTemplate messageTemplate;
	
	public CopyOnWriteArrayList<GameInstanceService> gameInstances = new CopyOnWriteArrayList<GameInstanceService>();
	
	@PostConstruct
	public void init() {
		gameInstanceRepository.deleteAll();
	}
	
	@GetMapping(value="/startGameInstance/{gameId}/{usernameId}")
	public ResponseEntity<String> startGameInstance(@PathVariable String gameId, @PathVariable String usernameId) {
		if(gameRepository.existsById(gameId) && usernameRepository.existsById(usernameId)) {
			GameInstanceService service = context.getBean(GameInstanceService.class);
			service.setupVariables(gameRepository.getOne(gameId), usernameRepository.getOne(usernameId), false);
			service.start();
			gameInstances.add(service);
			return ResponseEntity.status(HttpStatus.OK).body("");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("The game " + gameId + " username " + usernameId + " does not exist, so an insance could not be started!");
		}
	}
	
	@GetMapping(value="/stopGameInstance/{gameInstanceId}")
	public ResponseEntity<String> stopGameInstance(@PathVariable int gameInstanceId) {
		if(gameInstanceRepository.existsById(gameInstanceId)) {
			for(GameInstanceService instance : gameInstances) {
				if(instance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
					instance.shutdown();
					gameInstances.remove(instance);
					break;
				}
			}
			return ResponseEntity.status(HttpStatus.OK).body("");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("The game instance: " + gameInstanceId + " does not exist, so it could not be stopped!");
		}
	}
	
	@GetMapping(value="/startDebugGameInstance/{gameId}/{usernameId}/{restart}")
	public ResponseEntity<Integer> startDebugGameInstance(@PathVariable String gameId, @PathVariable String usernameId, @PathVariable Boolean restart) throws InterruptedException {
		if(gameRepository.existsById(gameId) && usernameRepository.existsById(usernameId)) {
			List<GameInstance> foundGameInstances = null;
			if(restart == false) {
				if((foundGameInstances = gameInstanceRepository.findByUsernameAndDebugInstance(new Username(usernameId, "", "", "", ""), true)).size() > 0) {
					for(GameInstanceService instance : gameInstances) {
						if(instance.getGameInstance().getGameInstanceId().equals(foundGameInstances.get(0).getGameInstanceId())) {
							return ResponseEntity.status(HttpStatus.OK).body(instance.getGameInstance().getGameInstanceId());
						}
					}
				}
			}
			if((foundGameInstances = gameInstanceRepository.findByUsernameAndDebugInstance(new Username(usernameId, "", "", "", ""), true)).size() > 0) {
				for(GameInstanceService instance : gameInstances) {
					if(instance.getGameInstance().getGameInstanceId().equals(foundGameInstances.get(0).getGameInstanceId())) {
						instance.shutdown();
						gameInstances.remove(instance);
						break;
					}
				}
				GameInstanceService service = context.getBean(GameInstanceService.class);
				Username username = new Username();
				username.setUsernameId(usernameId);
				service.setupVariables(gameRepository.getOne(gameId), usernameRepository.getOne(usernameId), true);
				service.start();
				gameInstances.add(service);
				Thread.sleep(500); //This really should not be done, but were gonna go with it
				return ResponseEntity.status(HttpStatus.OK).body(service.getGameInstance().getGameInstanceId());
			} else {
				GameInstanceService service = context.getBean(GameInstanceService.class);
				Username username = new Username();
				username.setUsernameId(usernameId);
				service.setupVariables(gameRepository.getOne(gameId), usernameRepository.getOne(usernameId), true);
				service.start();
				gameInstances.add(service);
				Thread.sleep(500); //This really should not be done, but were gonna go with it
				return ResponseEntity.status(HttpStatus.OK).body(service.getGameInstance().getGameInstanceId());
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-1);
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/connectToGameInstance/{usernameId}/{team}/{player}")
	public IMessage connectToGameInstance(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player) {
		for(GameInstanceService instance : gameInstances) {
			if(instance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				ConnectRequestMessage msg = new ConnectRequestMessage(); 
				msg.gameInstanceId = gameInstanceId;
				msg.usernameId = usernameId;
				msg.team = team;
				msg.player = player;
				messageTemplate.convertAndSend("/subscription/connectionResult/" + gameInstanceId + "/" + usernameId + "/" + team + "/" + player, instance.userConnect(msg));
			}
		}
		return null;
	}
	
	@MessageMapping("/gameInstance/{gameInstanceId}/disconnectFromGameInstance/{usernameId}/{team}/{player}")
	public IMessage disconnectFromGameInstance(@DestinationVariable int gameInstanceId, @DestinationVariable String usernameId, @DestinationVariable int team, @DestinationVariable int player) {
		for(GameInstanceService instance : gameInstances) {
			if(instance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
			   instance.userDisconnect(team, player);
			   messageTemplate.convertAndSend("/subscription/disconnectionResult/" + gameInstanceId + "/" + usernameId + "/" + team + "/" + player, new DisconnectResponseMessage());
			}
		}
		return null;
	}
	
	@GetMapping(value="/disconnectFromGameInstance")
	public String disconnectFromGameInstance() {
		return "";
	}
	
	@GetMapping("/playersAvaliable/{gameInstanceId}/{usernameId}")
	public ResponseEntity<List<PlayerAvaliableMessage>> playersAvailable(@PathVariable int gameInstanceId, @PathVariable String usernameId) {
		Optional<Username> username = usernameRepository.findById(usernameId);
		if(!username.isPresent()) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); }
		for(GameInstanceService gameInstance : gameInstances) {
			if(gameInstance.getGameInstance().getGameInstanceId().equals(gameInstanceId)) {
				return ResponseEntity.status(HttpStatus.OK).body(gameInstance.getTeamsAndPlayers(usernameId));
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	}

}
