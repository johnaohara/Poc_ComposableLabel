package io.hyperfoil.tools.exp.horreum.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.hyperfoil.tools.exp.horreum.entity.extractor.Extractor;
import io.hyperfoil.tools.exp.horreum.valid.ValidTarget;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(
        name = "label",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name","parent_id"})
        }
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Label extends PanacheEntity implements Comparable<Label> {

    public Label copy(Test test) {
        Label copyLabel =  new Label();
        copyLabel.name = this.name;
        copyLabel.uri = this.uri; //need to modify for the test
        copyLabel.parent_uri = this.uri;
        copyLabel.parent = test;
        copyLabel.target_schema = this.target_schema;
        copyLabel.multiType = this.multiType;
        copyLabel.scalarMethod = this.scalarMethod;
        copyLabel.parent_uri = this.uri;
        copyLabel.loadExtractors(this.extractors.stream().map(extractor -> Extractor.fromString(extractor.jsonpath).setName(extractor.name)).toArray(Extractor[]::new));
        return copyLabel;
    }

    public static enum MultiIterationType { Length, NxN}
    public static enum ScalarVariableMethod { First, All}


    @Pattern(regexp = "^[^{].*[^}]$",message = "Extractor names cannot start with '{' or end with '}'")
    @Pattern(regexp = "^[^$].+",message = "Extractor name cannot start with '$'")
    @Pattern(regexp = ".*(?<!\\[])$",message = "Extractor name cannot end with '[]'")
    public String name;

    @NotNull(message = "label must reference a test")
    @ManyToOne(cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    public Test parent;

    public String target_schema; //using a string to simplify the PoC

//    @NotNull  //disable for now, until we have more strict validation
    public String uri; //using a string to simplify the PoC
    public String parent_uri; //using a string to simplify the PoC

    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade = {CascadeType.PERSIST,CascadeType.MERGE}, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "parent")
    public List<@NotNull(message="null extractors are not supported") @ValidTarget Extractor> extractors;
    @ManyToOne(cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    public LabelReducer reducer;

    @Enumerated(EnumType.STRING)
    public MultiIterationType multiType = MultiIterationType.Length;
    @Enumerated(EnumType.STRING)
    public ScalarVariableMethod scalarMethod = ScalarVariableMethod.First;

    public Label(){}
    public Label(String name){
        this(name,null);
    }
    public Label(String name,Test parent){
        this(name,null,parent);
    }

    public Label(String name, String uri, Test parent){
        this.name = name;
        this.parent = parent;
        this.uri = uri;
        this.extractors = new ArrayList<>();
    }


    public Label loadExtractors(Extractor...extractors){
        this.extractors = Arrays.asList(extractors);
        this.extractors.forEach(e->e.parent=this);
        return this;
    }
    public Label setTargetSchema(String targetSchema){
        this.target_schema = targetSchema;
        return this;
    }

    public Label setReducer(String javascript){
        LabelReducer reducer = new LabelReducer(javascript);
        this.reducer = reducer;
        return this;
    }
    public Label setReducer(LabelReducer reducer){
        this.reducer = reducer;
        return this;
    }

    @Override
    public String toString(){return "label=[name:"+name+" id:"+id+" extractors="+(extractors==null?"null":extractors.stream().map(e->e.name).collect(Collectors.toList()))+"]";}


    @Override
    public boolean equals(Object o){
        if(!(o instanceof Label)){
            return false;
        }
        Label o1 = (Label)o;
        boolean rtrn = Objects.equals(this.id, o1.id) && this.name.equals(o1.name) && Objects.equals(this.parent,o1.parent);
        return rtrn;
    }
    @Override
    public int compareTo(Label o1) {
        int rtrn = 0;
        if(o1.usesLabelValueExtractor() && !this.usesLabelValueExtractor()){
            rtrn = -1;//o1 is less than 02
        }else if (this.usesLabelValueExtractor() && !o1.usesLabelValueExtractor()){
            rtrn = 1;//o2 is less than o1
        }else if (o1.dependsOn(this)){
            rtrn = -1;//o1 has to come after o2
        }else if (this.dependsOn(o1)) {
            rtrn = 1;//o1 must come before o2
        }else if (this.labelValueExtractorCount() > o1.labelValueExtractorCount()) {
            rtrn = 1;
        }else if ( o1.labelValueExtractorCount() > this.labelValueExtractorCount()){
            rtrn = -1;
        }else{
            //unable to compare them, assume "equal" rank?
        }
        return rtrn;
    }


    public long labelValueExtractorCount(){
        return extractors.stream().filter(e-> Extractor.Type.VALUE.equals(e.type)).count();
    }
    public long forEachCount(){
        return extractors.stream().filter(e->e.forEach).count();
    }
    public boolean hasForEach(){
        return extractors.stream().anyMatch(e->e.forEach);
    }
    public boolean usesOnlyJsonpathExtractor(){
        return extractors.stream().allMatch(e->Extractor.Type.PATH.equals(e.type));
    }
    public boolean usesLabelValueExtractor(){
        return extractors.stream().anyMatch(e->Extractor.Type.VALUE.equals(e.type));
    }
    public boolean dependsOn(Label l){
        //do not replace id == l.id with .equals because id can be null
        return extractors.stream().anyMatch(e->Extractor.Type.VALUE.equals(e.type) && e.targetLabel.id == l.id && (e).targetLabel.name.equals(l.name));
    }

    /**
     * returns true if this is part of a circular reference
     * @return
     */
    @JsonIgnore
    public boolean isCircular(){
        if(!usesLabelValueExtractor()){
            return false;
        }
        Queue<Label> todo = new PriorityQueue<>();
        extractors.stream()
                .filter(e->Extractor.Type.VALUE.equals(e.type))
                .map(e->e.targetLabel).forEach(todo::add);
        Label target;
        boolean ok = true;
        while( ok && (target = todo.poll()) !=null ){
            if(this.equals(target)){
                ok = false;
            }
            if(target.extractors!=null) {
                List<Label> targetLabels = target.extractors.stream()
                        .filter(e->Extractor.Type.VALUE.equals(e.type))
                        .map(e->e.targetLabel)
                        .toList();
                todo.addAll(targetLabels);
            }else{
                //extractors can be null for auto-created labels inside LabelValueExtactor :(
            }
        }
        return !ok;
    }
}
