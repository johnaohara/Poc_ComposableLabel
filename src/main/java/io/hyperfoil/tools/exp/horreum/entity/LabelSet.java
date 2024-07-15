package io.hyperfoil.tools.exp.horreum.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(
        name = "labelset",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"uri"})
        }
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelSet  extends PanacheEntity {

    String uri; //String for now - maybe we want to implement a global URI object
    String name;

    @OneToMany(cascade = {CascadeType.PERSIST,CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "id")
    Set<LabelSetEntry> labels;


    @Entity
    @Table(
            name = "label_set_entry",
            uniqueConstraints = {
                    @UniqueConstraint(columnNames = {"uri", "version"})
            }
    )
    public static class LabelSetEntry extends PanacheEntity {
        String uri;
        Integer version;

        @OneToOne(cascade = {CascadeType.PERSIST,CascadeType.MERGE})
        Label label;

    }
}
