
=======
import lombok.EqualsAndHashCode;

// Event organized by a shelter
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class EventEntity extends BaseEntity {

	// Internal event identifier
	private Integer eventId;

	// Name of the event
	private String name;

	// Date the event takes place
	@Temporal(TemporalType.DATE)
	private Date date;

	// Time the event starts
	private String time;

	// Where the event is held
	private String location;

}
