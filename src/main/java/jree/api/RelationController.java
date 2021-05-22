package jree.api;

public interface RelationController {

    RelationController ALWAYS_ACCEPT = new RelationController() {
        @Override
        public boolean validatePublishMessage(Session publisher, Recipient recipient, Relation relation) {
            return true;
        }
    };

    RelationController ALWAYS_REJECT = new RelationController() {
        @Override
        public boolean validatePublishMessage(Session publisher, Recipient recipient, Relation relation) {
            return false;
        }
    };

    boolean validatePublishMessage(Session publisher, Recipient recipient, Relation relation);

}
