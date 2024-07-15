package io.hyperfoil.tools.exp.horreum.entity;

import io.hyperfoil.tools.exp.horreum.entity.extractor.Extractor;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class LabelSetTest {

    @org.junit.jupiter.api.Test
    @Transactional
    public void create_simple_label_set(){
        Label l = new Label("test_label", "uri:global:label:simple_label:0", null);

        l.loadExtractors(
                Extractor.fromString("$.lve").setName("lve")
        );

        l.parent = new Test("dummy test"); //atm we need a dummy test to persist the label - we need to revisit this


        LabelSet.LabelSetEntry entry = new LabelSet.LabelSetEntry();
        entry.uri = "uri:global:labelSet:create_simple_label_set:label:test_label:0";
        entry.version = 0;
        entry.label = l;

        LabelSet labelSet = new LabelSet();
        labelSet.labels = Set.of(entry);
        labelSet.uri = "uri:global:labelSet:create_simple_label_set:0";
        labelSet.name = "testLabelSet";

        try {
            labelSet.persist();
        } catch (EntityExistsException e) {
            fail("should not already exist");
        }

    }

    @org.junit.jupiter.api.Test
    @Transactional
    public void populate_test_label_set(){

        Label l = new Label("test_label", "uri:global:label:test_label:0", null);

        l.name = "test_label";

        l.loadExtractors(
                Extractor.fromString("$.user").setName("user"),
                Extractor.fromString("$.uuid").setName("uuid")
        );

        l.parent = new Test("dummy test"); //atm we need a dummy test to persist the label - we need to revisit this

        LabelSet.LabelSetEntry entry = new LabelSet.LabelSetEntry();
        entry.uri = "uri:global:labelSet:populate_test_label_set:label:test_label:0";
        entry.version = 0;
        entry.label = l;

        LabelSet labelSet = new LabelSet();
        labelSet.labels = Set.of(entry);
        String labelSetUri = "uri:global:labelSet:populate_test_label_set:0";
        labelSet.uri = labelSetUri;
        labelSet.name = "testLabelSet";

        try {
            labelSet.persistAndFlush();
        } catch (EntityExistsException e) {
            fail("should not already exist");
        }

        Test test = new Test();
        test.name = "labelSetTest";
        test.copyLabelSet(labelSetUri);

        try {
            test.persistAndFlush();
        } catch (Exception e) {
            fail("could not persist test", e);
        }

        Test newTest = Test.findById(test.id);

        assertNotEquals(0, newTest.labels.size());
    }

    @org.junit.jupiter.api.Test
    public void breakpoint(){
        System.out.println("breakpoint");

    }
}
