package edu.connexion3a36.models;

import edu.connexion3a36.entities.Cours;
import javafx.beans.property.*;

public class Course {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty titre = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty image = new SimpleStringProperty();
    private final ObjectProperty<Cours> entity = new SimpleObjectProperty<>();

    public Course(Cours cours) {
        setId(cours.getId());
        setTitre(cours.getTitre());
        setDescription(cours.getDescription());
        setImage(cours.getImage());
        setEntity(cours);
    }

    public long getId() { return id.get(); }
    public LongProperty idProperty() { return id; }
    public void setId(long id) { this.id.set(id); }

    public String getTitre() { return titre.get(); }
    public StringProperty titreProperty() { return titre; }
    public void setTitre(String titre) { this.titre.set(titre); }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }
    public void setDescription(String description) { this.description.set(description); }

    public String getImage() { return image.get(); }
    public StringProperty imageProperty() { return image; }
    public void setImage(String image) { this.image.set(image); }

    public Cours getEntity() { return entity.get(); }
    public ObjectProperty<Cours> entityProperty() { return entity; }
    public void setEntity(Cours entity) { this.entity.set(entity); }
}