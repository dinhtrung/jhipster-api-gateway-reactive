package com.ft.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.ft.domain.Node;
import com.ft.repository.NodeRepository;
import com.ft.web.rest.errors.BadRequestAlertException;
import com.ft.web.rest.errors.EmailAlreadyUsedException;
import com.ft.web.rest.errors.LoginAlreadyUsedException;
import com.querydsl.core.types.Predicate;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing nodes.
 * <p>
 * This class accesses the {@link Node} entity, and needs to fetch its collection of authorities.
 * <p>
 * For a normal use-case, it would be better to have an eager relationship between Node and Authority,
 * and send everything to the client side: there would be no View Model and DTO, a lot less code, and an outer-join
 * which would be good for performance.
 * <p>
 * We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the node and the authorities, because people will
 * quite often do relationships with the node, and we don't want them to get the authorities all
 * the time for nothing (for performance reasons). This is the #1 goal: we should not impact our nodes'
 * application because of this use-case.</li>
 * <li> Not having an outer join causes n+1 requests to the database. This is not a real issue as
 * we have by default a second-level cache. This means on the first HTTP call we do the n+1 requests,
 * but then all authorities come from the cache, so in fact it's much better than doing an outer join
 * (which will get lots of data from the database, for each HTTP call).</li>
 * <li> As this manages nodes, for security reasons, we'd rather have a DTO layer.</li>
 * </ul>
 * <p>
 * Another option would be to have a specific JPA entity graph to handle this case.
 */
@RestController
@RequestMapping("/api")
public class NodeResource {

    private final Logger log = LoggerFactory.getLogger(NodeResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;
    
    private static final String ENTITY_NAME = "node";

    private final NodeRepository nodeRepository;

    public NodeResource(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    /**
     * {@code POST  /nodes}  : Creates a new node.
     * <p>
     * Creates a new node if the login and email are not already used, and sends an
     * mail with an activation link.
     * The node needs to be activated on creation.
     *
     * @param node the node to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new node, or with status {@code 400 (Bad Request)} if the login or email is already in use.
     * @throws BadRequestAlertException {@code 400 (Bad Request)} if the login or email is already in use.
     */
    @PostMapping("/nodes")
    public Mono<ResponseEntity<Node>> createNode(@Valid @RequestBody Node node) {
        log.debug("REST request to save Node : {}", node);

        if (node.getId() != null) {
            throw new BadRequestAlertException("A new node cannot already have an ID", ENTITY_NAME, "idexists");
            // Lowercase the node login before comparing with database
        }
        return nodeRepository.save(node)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/nodes/" + result.getId()))
                        .headers(HeaderUtil.createAlert(applicationName, "nodeManagement.created", node.getId()))
                        .body(node);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT /nodes} : Updates an existing Node.
     *
     * @param node the node to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated node.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already in use.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already in use.
     */
    @PutMapping("/nodes")
    public Mono<ResponseEntity<Node>> updateNode(@Valid @RequestBody Node node) {
        log.debug("REST request to update Node : {}", node);
        if (node.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        return nodeRepository.save(node)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
            .map(result -> ResponseEntity.ok()
                .headers(HeaderUtil.createAlert(applicationName, ENTITY_NAME, result.getId()))
                .body(result)
            );
    }

    /**
     * {@code GET /nodes} : get all nodes.
     *
     * @param request a {@link ServerHttpRequest} request.
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all nodes.
     */
    @GetMapping("/nodes")
    public Mono<ResponseEntity<Flux<Node>>> getAllNodes(ServerHttpRequest request, Pageable pageable) {
        return nodeRepository.count()
            .map(total -> new PageImpl<>(new ArrayList<>(), pageable, total))
            .map(page -> PaginationUtil.generatePaginationHttpHeaders(UriComponentsBuilder.fromHttpRequest(request), page))
            .map(headers -> ResponseEntity.ok().headers(headers).body(nodeRepository.findAll(pageable.getSort())));
    }
    
    @GetMapping("/search/nodes")
    public Mono<ResponseEntity<Flux<Node>>> getAllNodes(Predicate predicate, Sort sort) {
    	log.debug("search for node: sort {} predicate {}", sort, predicate);
        return Mono.just(ResponseEntity.ok(nodeRepository.findAll(predicate, sort)));
    }


    /**
     * {@code GET /nodes/:login} : get the "login" node.
     *
     * @param login the login of the node to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the "login" node, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/nodes/{id}")
    public Mono<Node> getNode(@PathVariable String id) {
        log.debug("REST request to get Node : {}", id);
        return nodeRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    /**
     * {@code DELETE /nodes/:login} : delete the "login" Node.
     *
     * @param login the login of the node to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/nodes/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteNode(@PathVariable String id) {
        log.debug("REST request to delete Node: {}", id);
        return nodeRepository.deleteById(id)
            .map(it -> ResponseEntity.noContent().headers(HeaderUtil.createAlert( applicationName, "nodeManagement.deleted", id)).build());
    }
}
