package jree.api;

public interface RelationController {

    RelationController ALWAYS_ACCEPT = new RelationController() {
        @Override
        public boolean validatePublishMessage(Relation relation) {
            return true;
        }
    };

    boolean validatePublishMessage(Relation relation);

}
