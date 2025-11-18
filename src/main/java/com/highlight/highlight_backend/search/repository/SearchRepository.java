package com.highlight.highlight_backend.search.repository;

import com.highlight.highlight_backend.admin.auction.domain.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

    @Query("SELECT a FROM Auction a WHERE a.id = :auctionId")
    Auction findOne (@Param("auctionId")Long auctionId);
}