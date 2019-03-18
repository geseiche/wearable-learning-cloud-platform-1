package wlcp.model.master;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Entity implementation class for Entity: GameInstance
 *
 */
@Entity
@Table(name = "GAME_INSTANCE")
public class GameInstance implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "GAME_INSTANCE_ID")
	private Integer gameInstanceId;
	
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "GAME", referencedColumnName = "GAME_ID")
	private Game game;
	
	@OneToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "USERNAME", referencedColumnName = "USERNAME_ID")
	private Username username;
	
	@Column(name = "DEBUG_INSTANCE")
	private boolean debugInstance;
	
	public GameInstance() {
		super();
	}

	public GameInstance(Game game, Username username, boolean debugInstance) {
		super();
		this.game = game;
		this.username = username;
		this.debugInstance = debugInstance;
	}

	public Integer getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(Integer gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public Username getUsername() {
		return username;
	}

	public void setUsername(Username username) {
		this.username = username;
	}

	public boolean isDebugInstance() {
		return debugInstance;
	}

	public void setDebugInstance(boolean debugInstance) {
		this.debugInstance = debugInstance;
	}
   
}
